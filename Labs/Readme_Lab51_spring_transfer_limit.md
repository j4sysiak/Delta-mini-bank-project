# Lab 51 — Spring Boot: egzekwowanie konfigurowalnego limitu przelewu

## Dlaczego ten temat?

W Labie 50 utworzyłeś `BankTransferProperties` i testowałeś bindowanie
konfiguracji. Sama właściwość `bank.transfer.max-amount` nie ogranicza
jednak przelewów. Teraz wykorzystasz ją w istniejącym
`BankAccountService` i sprawdzisz zachowanie zarówno jednostkowo,
jak i w kontekście Springa z PostgreSQL.

Zachowujemy Java 21, Spring Boot 3.4.5, Groovy z `localGroovy()`
i Spock 2.3 dla Groovy 3.0. Nie zmieniaj Gradle ani migracji.
Zmiany opisane w tym README wykonujesz samodzielnie.

## 1. Kontrakt nowej reguły

Przyjmij następujące zasady:

- nowy przelew z kwotą większą niż `maxAmount` jest odrzucany,
- kwota równa limitowi jest dozwolona — porównujemy przez `compareTo`,
- sprawdzenie limitu następuje przed pobraniem i blokowaniem kont,
- duplikat już wykonanego przelewu nadal zwraca `false`, nawet jeśli
  od jego wykonania limit został obniżony,
- reguła dotyczy tylko `transfer`, nie `deposit` ani `withdraw`.

Dlatego sprawdzenie limitu umieścisz **po istniejącym sprawdzeniu
idempotencji**, ale **przed zapytaniem blokującym konta**.
Zachowaj wcześniejszą walidację dodatniej kwoty, różnych kont i `requestId`.

Limit w tym ćwiczeniu jest limitem liczbowym na jedną operację w walucie
kont. Nie jest limitem dziennym ani przeliczeniem między walutami.
Istniejące odrzucanie przelewu między kontami o różnych walutach pozostaje.
Nie wykorzystuj `defaultCurrency` do nadpisywania waluty istniejącego konta.

## 2. Wstrzyknięcie konfiguracji do serwisu

W pliku:
`src\main\java\com\delta\bank\application\service\BankAccountService.java`

dodaj import:

```java
import com.delta.bank.config.BankTransferProperties;
```

Dodaj pole i zastąp istniejący konstruktor poniższym:

```java
private final BankTransferProperties transferProperties;

public BankAccountService(BankAccountRepository repository,
                          AccountTransactionRepository transactionRepository,
                          BankTransferProperties transferProperties) {
    this.repository = repository;
    this.transactionRepository = transactionRepository;
    this.transferProperties = java.util.Objects.requireNonNull(
            transferProperties, "transferProperties");
}
```

Pozostaw istniejące pola repozytoriów. Przy jednym konstruktorze Spring
nie wymaga `@Autowired`. Bean konfiguracji jest już zarejestrowany przez
`@EnableConfigurationProperties(BankTransferProperties.class)` na
`DeltaApplication` z Laba 50.

Nie dodawaj drugiego konstruktora, który sam tworzy domyślną konfigurację:
ukrywałby brak zależności i mógłby pomijać ustawienia aplikacji.

## 3. Kontrola limitu w `transfer`

Bezpośrednio po istniejącym bloku:

```java
if (transactionRepository.findFirstByTransferRequestIdAndAccountNumberAndType(
        requestId, fromNumber, TransactionType.TRANSFER_OUT
).isPresent()) {
    return false;
}
```

dodaj:

```java
if (amount.compareTo(transferProperties.getMaxAmount()) > 0) {
    throw new IllegalArgumentException("Transfer amount exceeds configured limit");
}
```

Nie zmieniaj pozostałego przebiegu przelewu ani `@Transactional`.
`GlobalExceptionHandler` już obsługuje `IllegalArgumentException`:
klient HTTP otrzyma `400` z kodem `INVALID_REQUEST`.
Nie potrzeba nowego handlera ani zmiany formatu odpowiedzi.

Wstrzyknięta konfiguracja służy jako ustawienia odczytywane przez serwis.
Nie zmieniaj jej setterami w trakcie obsługi żądań. Zwykłe
`@ConfigurationProperties` nie oznacza automatycznego odświeżania
ustawień z pliku podczas działania aplikacji.

## 4. Zaktualizuj istniejące testy korzystające z konstruktora

Zmiana konstruktora wymaga aktualizacji **wszystkich** bezpośrednich
wywołań `new BankAccountService(...)`. W obecnym projekcie są w:

- `src\test\groovy\com\delta\bank\application\service\BankAccountServiceUnitSpec.groovy`,
- `src\test\groovy\com\delta\bank\SpockMocksSpec.groovy` — cztery wywołania.

Dodaj do obu plików import:

```groovy
import com.delta.bank.config.BankTransferProperties
```

Przekaż trzeci argument, np.:

```groovy
service = new BankAccountService(
        accountRepository, transactionRepository, new BankTransferProperties())
```

W `SpockMocksSpec` zachowaj lokalne nazwy `repository`,
`transactionRepository` i `def service`; dodaj tylko trzeci argument.
Domyślny limit to `5000.00`. Nie usuwaj wcześniejszych testów.

Testy korzystające z `@Autowired BankAccountService` otrzymają zależność
od Springa. Test MVC z Laba 47 używa mocka całego serwisu przez
`@SpringBean`, więc nie potrzebuje dodatkowego beana konfiguracji.

## 5. Nowe testy jednostkowe reguły

Utwórz:
`src\test\groovy\com\delta\bank\application\service\TransferLimitUnitSpec.groovy`

```groovy
package com.delta.bank.application.service

import com.delta.bank.config.BankTransferProperties
import com.delta.bank.domain.AccountTransactionEntity
import com.delta.bank.domain.AccountTransactionRepository
import com.delta.bank.domain.BankAccountEntity
import com.delta.bank.domain.BankAccountRepository
import com.delta.bank.domain.TransactionType
import spock.lang.Specification
import spock.lang.Unroll

class TransferLimitUnitSpec extends Specification {

    BankAccountRepository accounts = Mock()
    AccountTransactionRepository transactions = Mock()
    BankTransferProperties properties = new BankTransferProperties()
    BankAccountService service

    def setup() {
        properties.maxAmount = new BigDecimal("100.00")
        service = new BankAccountService(accounts, transactions, properties)
    }

    def "new over-limit transfer is rejected without locking or saving accounts"() {
        when:
        service.transfer("limit-reject", "PLN-1", "PLN-2", new BigDecimal("100.01"))

        then:
        def error = thrown(IllegalArgumentException)
        error.message == "Transfer amount exceeds configured limit"
        1 * transactions.findFirstByTransferRequestIdAndAccountNumberAndType(
                "limit-reject", "PLN-1", TransactionType.TRANSFER_OUT) >> Optional.empty()
        0 * accounts._
        0 * transactions.save(_)
    }

    @Unroll
    def "new transfer of #amount is allowed at or below the limit"() {
        given:
        def from = new BankAccountEntity("PLN-1", "Alice", new BigDecimal("200.00"), "PLN")
        def to = new BankAccountEntity("PLN-2", "Bob", BigDecimal.ZERO, "PLN")

        when:
        boolean executed = service.transfer("limit-allow", "PLN-1", "PLN-2", amount)

        then:
        1 * transactions.findFirstByTransferRequestIdAndAccountNumberAndType(
                "limit-allow", "PLN-1", TransactionType.TRANSFER_OUT) >> Optional.empty()
        1 * accounts.findAllForUpdateOrderByNumber(["PLN-1", "PLN-2"]) >> [from, to]
        1 * accounts.save(from)
        1 * accounts.save(to)
        2 * transactions.save(_)
        executed
        from.balance.compareTo(new BigDecimal("200.00").subtract(amount)) == 0
        to.balance.compareTo(amount) == 0

        where:
        amount << [new BigDecimal("99.99"), new BigDecimal("100.0")]
    }

    def "completed duplicate remains a duplicate above the current limit"() {
        given:
        def existing = new AccountTransactionEntity(
                "PLN-1", TransactionType.TRANSFER_OUT, new BigDecimal("150.00"),
                "PLN", "Transfer to PLN-2", "limit-duplicate")

        when:
        boolean executed = service.transfer(
                "limit-duplicate", "PLN-1", "PLN-2", new BigDecimal("150.00"))

        then:
        1 * transactions.findFirstByTransferRequestIdAndAccountNumberAndType(
                "limit-duplicate", "PLN-1", TransactionType.TRANSFER_OUT) >> Optional.of(existing)
        0 * accounts._
        0 * transactions.save(_)
        !executed
    }
}
```

Test z `100.0` sprawdza dokładnie granicę, mimo innej skali niż
konfiguracja `100.00`. Test duplikatu chroni przed przypadkową zmianą
kolejności reguł.

Zachowujemy obecną semantykę idempotencji. Ten lab nie rozwiązuje
problemu użycia tego samego `requestId` z inną kwotą lub innym odbiorcą
ani wyścigów równoczesnych żądań — to oddzielny temat.

## 6. Test integracyjny: Spring dostarcza ustawiony limit

Utwórz:
`src\test\groovy\com\delta\bank\TransferLimitIntegrationSpec.groovy`

```groovy
package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import com.delta.bank.bootstrap.DeltaApplication
import com.delta.bank.config.BankTransferProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
        classes = DeltaApplication,
        properties = ["bank.transfer.max-amount=100.00"])
class TransferLimitIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    @Autowired
    BankTransferProperties properties

    def "configured limit rejects a transfer without changing stored data"() {
        given:
        service.openAccount("PLN-5101", "Alice", new BigDecimal("200.00"), "PLN")
        service.openAccount("PLN-5102", "Bob", new BigDecimal("20.00"), "PLN")

        expect:
        properties.maxAmount.compareTo(new BigDecimal("100.00")) == 0

        when:
        service.transfer("lab51-reject", "PLN-5101", "PLN-5102", new BigDecimal("100.01"))

        then:
        thrown(IllegalArgumentException)
        jdbcTemplate.queryForObject(
                "select balance from bank_accounts where number = ?",
                BigDecimal, "PLN-5101").compareTo(new BigDecimal("200.00")) == 0
        jdbcTemplate.queryForObject(
                "select balance from bank_accounts where number = ?",
                BigDecimal, "PLN-5102").compareTo(new BigDecimal("20.00")) == 0
        jdbcTemplate.queryForObject(
                "select count(*) from bank_transactions where transfer_request_id = ?",
                Long, "lab51-reject") == 0L
    }
}
```

Właściwość podana na teście nadpisuje `5000.00` z `application.yml`
tylko dla tego kontekstu testowego. Nie zmieniaj globalnego YAML,
aby inne testy nie dostały przypadkiem limitu `100.00`.
Nie dodawaj `@Transactional` do tej specyfikacji.

## Uruchomienie

Najpierw testy jednostkowe i wcześniejszy test bindowania konfiguracji:

```powershell
.\gradlew.bat test --tests "com.delta.bank.application.service.TransferLimitUnitSpec" --tests "com.delta.bank.application.service.BankAccountServiceUnitSpec" --tests "com.delta.bank.SpockMocksSpec" --tests "com.delta.bank.config.BankTransferPropertiesSpec"
```

Następnie test integracyjny z testową bazą PostgreSQL:

```powershell
docker compose -f docker-compose.test.yml up -d --wait postgres-test
.\gradlew.bat test --tests "com.delta.bank.TransferLimitIntegrationSpec" --tests "com.delta.bank.TransferTransactionBoundarySpec" --tests "com.delta.bank.TransferIdempotencySpec"
```

W Git Bash użyj `./gradlew` zamiast `.\gradlew.bat`, zachowując selektory.
Jeśli nazwa Twojej specyfikacji z Laba 48 jest inna, dostosuj jej selektor.

**Uwaga:** `BaseIntegrationSpec` czyści tabele w `delta_test` na porcie
`5433`. Nie podłączaj go do bazy produkcyjnej ani nie uruchamiaj
równolegle innych testów czyszczących tę samą bazę.

## Eksperymenty

1. Zmień limit w teście jednostkowym na `50.00` i dopasuj przypadki
   graniczne. Czy musisz zmieniać kod serwisu?
2. Dodaj test MVC, w którym mock `service.transfer` rzuca wyjątek
   limitu. Sprawdź `400` i `INVALID_REQUEST`. Ten test sprawdza mapowanie
   błędu, nie faktyczne egzekwowanie limitu.
3. Dodaj test integracyjny poprawnego przelewu dokładnie na granicy
   `100.00`, sprawdzając oba salda oraz dwa wpisy historii.
4. Rozważ konfigurację limitów per waluta. Dlaczego jeden limit
   liczbowy nie oznacza tej samej wartości ekonomicznej dla PLN i EUR?

## Pytania kontrolne

1. Dlaczego sprawdzamy duplikat przed limitem, a limit przed blokadą kont?
2. Dlaczego testy tworzące serwis przez `new` muszą jawnie przekazać konfigurację?
3. Co udowadnia test integracyjny, czego nie dowodzi sam test mocków?
4. Dlaczego nie zmieniamy globalnego YAML na potrzeby pojedynczego testu?
5. Czy ten limit jest kontrolą uprawnień albo limitem dziennym? Czego jeszcze by wymagały?

## Do zapamiętania

- Konfigurację trzeba włączyć do rzeczywistej ścieżki logiki biznesowej.
- Reguły muszą mieć świadomie dobraną kolejność.
- Testuj wartości poniżej, na granicy i powyżej limitu oraz istniejące zachowanie duplikatów.
- Konstruktor ujawnia zależności; testy Springa i testy jednostkowe dostarczają je w różny sposób.
