# Lab 48 — Spring Boot: granice transakcji i rollback przelewu

## Dlaczego ten temat?

Lab 47 izolował warstwę HTTP od bazy danych. Teraz sprawdzisz realną
współpracę Springa, JPA i PostgreSQL: czy salda oraz historia przelewu
są zatwierdzane lub wycofywane jako jedna całość.

`BankAccountService.transfer(...)` ma już `@Transactional`. Nie dodawaj
drugiej implementacji przelewu. Zbudujesz testy istniejącej metody oraz
poznasz `TransactionTemplate`, domyślne `Propagation.REQUIRED` i różnicę
między wyjątkiem w transakcji a wyjątkiem po jej zakończeniu.

Zachowujemy Java 21, Spring Boot 3.4.5, Groovy dostarczane przez
`localGroovy()` i Spock 2.3 dla Groovy 3.0. Nie zmieniaj Gradle,
kodu produkcyjnego, migracji ani istniejących testów. Dodaj tylko
specyfikację opisaną poniżej. Sam lab nie wymaga funkcji preview;
obecna konfiguracja projektu pozostaje bez zmian.

## 1. Przygotowanie: gdzie jest granica transakcji?

Przeczytaj istniejące pliki:

- `src\main\java\com\delta\bank\application\service\BankAccountService.java`,
- `src\main\java\com\delta\bank\domain\BankAccountRepository.java`,
- `src\test\groovy\com\delta\bank\BaseIntegrationSpec.groovy`.

Zwróć uwagę, że `transfer` zmienia oba salda i zapisuje dwa wpisy historii.
Domyślna propagacja `@Transactional` to `REQUIRED`:

- jeżeli nie ma transakcji, proxy Springa rozpoczyna nową,
- jeżeli transakcja już istnieje, metoda do niej dołącza.

Wywołuj serwis wstrzyknięty przez Springa, nie twórz go przez `new`.
Test nie może mieć `@Transactional` na klasie ani metodach: chcemy
samodzielnie kontrolować granice i odczytać stan po ich zakończeniu.

## 2. Specyfikacja integracyjna

Utwórz plik:
`src\test\groovy\com\delta\bank\TransferTransactionBoundarySpec.groovy`

```groovy
package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class TransferTransactionBoundarySpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    @Autowired
    PlatformTransactionManager transactionManager

    private static final String FROM = "PLN-4801"
    private static final String TO = "PLN-4802"

    def "successful transfer commits balances and both history entries"() {
        given:
        openAccounts()

        when:
        boolean executed = service.transfer(
                "lab48-success", FROM, TO, new BigDecimal("25.00"))

        then:
        executed
        balanceOf(FROM).compareTo(new BigDecimal("75.00")) == 0
        balanceOf(TO).compareTo(new BigDecimal("45.00")) == 0
        historyFor("lab48-success") == [
                [account: FROM, type: "TRANSFER_OUT", amount: new BigDecimal("25.00")],
                [account: TO, type: "TRANSFER_IN", amount: new BigDecimal("25.00")]
        ]
    }

    def "failure after transfer rolls back the surrounding transaction"() {
        given:
        openAccounts()
        def transaction = new TransactionTemplate(transactionManager)

        when:
        transaction.executeWithoutResult { status ->
            service.transfer("lab48-rollback", FROM, TO, new BigDecimal("25.00"))
            throw new IllegalStateException("failure after transfer")
        }

        then:
        def error = thrown(IllegalStateException)
        error.message == "failure after transfer"
        balanceOf(FROM).compareTo(new BigDecimal("100.00")) == 0
        balanceOf(TO).compareTo(new BigDecimal("20.00")) == 0
        historyFor("lab48-rollback").isEmpty()
    }

    def "exception outside a completed transaction does not undo the transfer"() {
        given:
        openAccounts()

        when:
        service.transfer("lab48-already-committed", FROM, TO, new BigDecimal("25.00"))
        throw new IllegalStateException("failure outside transaction")

        then:
        thrown(IllegalStateException)
        balanceOf(FROM).compareTo(new BigDecimal("75.00")) == 0
        balanceOf(TO).compareTo(new BigDecimal("45.00")) == 0
        historyFor("lab48-already-committed").size() == 2
    }

    private void openAccounts() {
        service.openAccount(FROM, "Alice", new BigDecimal("100.00"), "PLN")
        service.openAccount(TO, "Bob", new BigDecimal("20.00"), "PLN")
    }

    private BigDecimal balanceOf(String number) {
        return jdbcTemplate.queryForObject(
                "select balance from bank_accounts where number = ?",
                BigDecimal,
                number)
    }

    private List<Map<String, Object>> historyFor(String requestId) {
        return jdbcTemplate.queryForList("""
                select account_number, type, amount
                from bank_transactions
                where transfer_request_id = ?
                order by account_number
                """, requestId).collect { row ->
            [account: row.account_number, type: row.type, amount: row.amount]
        }
    }
}
```

Nie dodawaj własnego `setup` czyszczącego bazę. `BaseIntegrationSpec`
już wykonuje `TRUNCATE` tabel przed każdym testem. Otwarcie kont tworzy
wpisy `DEPOSIT`, dlatego historię przelewu filtrujemy po jego `requestId`,
zamiast oczekiwać tylko dwóch rekordów w całej tabeli.

## 3. Co dokładnie wykazują testy?

| Test | Granica transakcji | Oczekiwany wynik |
|------|--------------------|-----------------|
| Udany przelew | Proxy serwisu rozpoczyna i kończy transakcję | Salda i historia zapisane |
| Wyjątek wewnątrz `TransactionTemplate` | Serwis dołącza do transakcji otaczającej | Salda i historia wycofane |
| Wyjątek po powrocie z serwisu, bez transakcji otaczającej | Transakcja serwisu już zakończona | Przelew pozostaje zapisany |

Drugi test wywołuje błąd **po wykonaniu logiki przelewu**, ale jeszcze
przed zatwierdzeniem transakcji otaczającej. To ważniejsze niż wyjątek
walidacji rzucony przed pierwszą zmianą salda.

Ten test dowodzi rollbacku wspólnej transakcji. **Sam nie dowodzi**,
że `@Transactional` na `transfer` jest niezbędne: transakcję uruchomił
test. Nie traktuj go jako dowodu, że usunięcie adnotacji z serwisu jest
bezpieczne. W produkcji klient nie musi otwierać transakcji za serwis.

Po zakończeniu transakcji odczytujemy dane przez `JdbcTemplate`, a nie
sprawdzamy wyłącznie referencji do encji w pamięci. Rollback bazy nie
oznacza automatycznego odtworzenia wartości pól każdego obiektu Java.

## Uruchomienie

**Uwaga:** `BaseIntegrationSpec` usuwa dane z tabel testowej bazy
`delta_test` na porcie `5433`. Nie kieruj go na bazę roboczą lub
produkcyjną. Nie uruchamiaj równolegle innych specyfikacji używających
tej samej bazy i czyszczenia przez `TRUNCATE`.

Uruchom Docker Desktop, a następnie z katalogu projektu:

```powershell
docker compose -f docker-compose.test.yml up -d --wait postgres-test
.\gradlew.bat test --tests "com.delta.bank.TransferTransactionBoundarySpec"
```

W Git Bash po uruchomieniu bazy:

```bash
./gradlew test --tests "com.delta.bank.TransferTransactionBoundarySpec"
```

Nie potrzebujesz uruchomionej aplikacji na porcie 8080.
`BaseIntegrationSpec` startuje kontekst Spring Boot i uruchamia Flyway.
W przeciwieństwie do Laba 47 jest to pełny test integracyjny z JPA
i PostgreSQL, a nie wycinek MVC.

## Eksperymenty

1. W drugim teście zamiast wyjątku wywołaj `status.setRollbackOnly()`
   i pozwól callbackowi normalnie się zakończyć. Sprawdź salda i historię
   po `executeWithoutResult`. Czy do rollbacku zawsze potrzebny jest wyjątek?
2. Dodaj test odrzucenia przelewu z powodu niewystarczających środków.
   Sprawdź nie tylko wyjątek, lecz również oba salda i brak historii
   dla danego `requestId`.
3. W nowym teście złap własny wyjątek **wewnątrz callbacka**, po udanym
   `service.transfer`, i nie oznaczaj transakcji jako rollback-only.
   Czy transakcja się zatwierdzi? Porównaj to z wyjątkiem wychodzącym
   z metody serwisu przez proxy transakcyjne.
4. Narysuj granice transakcji dla wszystkich trzech testów. Gdzie
   następuje commit, a gdzie rollback?

## Jak o tym opowiedzieć

`@Transactional` deklaruje granicę transakcji, zwykle realizowaną przez
proxy Springa. Domyślne `REQUIRED` dołącza do istniejącej transakcji
albo rozpoczyna nową. Dla nowej transakcji commit następuje po poprawnym
zakończeniu wywołania, zanim klient otrzyma wynik z proxy.

Domyślnie wyjątki typu `RuntimeException` i `Error` wychodzące przez
interceptor transakcyjny powodują rollback; dla wyjątków checked
potrzebna może być reguła `rollbackFor`. `TransactionTemplate` jest
programowym sposobem zarządzania transakcją — w tym labie pozwala
kontrolować granicę bez dodawania produkcyjnego serwisu pomocniczego.

`save()` nie jest synonimem commit. SQL może wykonać się wcześniej,
np. przy flush, ale transakcja nadal może zostać wycofana.
Nie stosuj `REQUIRES_NEW` jako uniwersalnej „naprawy”: niezależny commit
może zniszczyć oczekiwaną atomowość operacji.

## Pytania kontrolne

1. Dlaczego wyjątek po powrocie z serwisu nie cofa już zatwierdzonego przelewu?
2. Czym `save`, flush i commit różnią się od siebie?
3. Dlaczego testu z tego laba nie oznaczamy jako `@Transactional`?
4. Co zmienia domyślna propagacja `REQUIRED`, gdy transakcja już istnieje?
5. Dlaczego sprawdzenie tylko obiektu zwróconego przez serwis nie dowodzi trwałości danych?

## Do zapamiętania

- Salda i historia przelewu powinny należeć do jednej transakcji.
- Testuj stan bazy po zakończeniu granicy transakcyjnej.
- Wyjątek musi wystąpić w odpowiedniej granicy, aby spowodować rollback.
- Test HTTP z mockiem serwisu nie zastępuje testu transakcji w PostgreSQL.
