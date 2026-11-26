# Lab 49 — Testy jednostkowe serwisu Springa z mockami Spocka

## Dlaczego ten temat?

Lab 47 sprawdzał warstwę HTTP przez `@WebMvcTest`, a Lab 48 granice
transakcji w PostgreSQL. Teraz przetestujesz samą logikę
`BankAccountService`, izolując go od Spring context i bazy danych.

Użyjesz mocków repozytoriów ze Spocka, aby sprawdzić decyzje serwisu:
walidację kwoty, idempotentny duplikat przelewu i odmowę przelewu bez
wystarczających środków.

Projekt pozostaje na Java 21, Spring Boot 3.4.5, Groovy dostarczanym
przez `localGroovy()` i Spock 2.3 dla Groovy 3.0. Nie zmieniaj Gradle
ani kodu produkcyjnego. Dodaj tylko nową specyfikację.

## 1. Specyfikacja jednostkowa

Utwórz plik:
`src\test\groovy\com\delta\bank\application\service\BankAccountServiceUnitSpec.groovy`

```groovy
package com.delta.bank.application.service

import com.delta.bank.domain.AccountTransactionEntity
import com.delta.bank.domain.AccountTransactionRepository
import com.delta.bank.domain.BankAccountEntity
import com.delta.bank.domain.BankAccountRepository
import com.delta.bank.domain.TransactionType
import spock.lang.Specification

import java.util.Optional

class BankAccountServiceUnitSpec extends Specification {

    BankAccountRepository accountRepository = Mock()
    AccountTransactionRepository transactionRepository = Mock()
    BankAccountService service

    def setup() {
        service = new BankAccountService(accountRepository, transactionRepository)
    }

    def "deposit rejects a non-positive amount before accessing repositories"() {
        when:
        service.deposit("req-unit-1", "PLN-1", BigDecimal.ZERO)

        then:
        thrown(IllegalArgumentException)
        0 * accountRepository._
        0 * transactionRepository._
    }

    def "duplicate transfer request returns false without locking or saving accounts"() {
        given:
        def existing = new AccountTransactionEntity(
                "PLN-1",
                TransactionType.TRANSFER_OUT,
                new BigDecimal("25.00"),
                "PLN",
                "Transfer to PLN-2",
                "req-unit-2")

        when:
        boolean executed = service.transfer(
                "req-unit-2", "PLN-1", "PLN-2", new BigDecimal("25.00"))

        then:
        1 * transactionRepository.findFirstByTransferRequestIdAndAccountNumberAndType(
                "req-unit-2", "PLN-1", TransactionType.TRANSFER_OUT
        ) >> Optional.of(existing)
        0 * accountRepository._
        0 * transactionRepository.save(_)
        !executed
    }

    def "transfer with insufficient funds fails before persisting changes"() {
        given:
        def from = new BankAccountEntity(
                "PLN-1", "Alice", new BigDecimal("10.00"), "PLN")
        def to = new BankAccountEntity(
                "PLN-2", "Bob", new BigDecimal("20.00"), "PLN")

        when:
        service.transfer("req-unit-3", "PLN-1", "PLN-2", new BigDecimal("25.00"))

        then:
        def error = thrown(IllegalStateException)
        error.message == "Insufficient funds for transfer"

        1 * transactionRepository.findFirstByTransferRequestIdAndAccountNumberAndType(
                "req-unit-3", "PLN-1", TransactionType.TRANSFER_OUT
        ) >> Optional.empty()
        1 * accountRepository.findAllForUpdateOrderByNumber(["PLN-1", "PLN-2"]) >> [from, to]
        0 * accountRepository.save(_)
        0 * transactionRepository.save(_)

        from.balance.compareTo(new BigDecimal("10.00")) == 0
        to.balance.compareTo(new BigDecimal("20.00")) == 0
    }
}
```

Mocki są zwykłymi obiektami Spocka — `BankAccountService` tworzysz
bezpośrednio przez konstruktor, więc Spring nie tworzy kontekstu ani
proxy transakcyjnego. W testach interakcje `0 * ...` potwierdzają,
że serwis nie próbuje zapisywać danych w scenariuszu odrzuconym.

## 2. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.application.service.BankAccountServiceUnitSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.application.service.BankAccountServiceUnitSpec"
```

Ta specyfikacja nie dziedziczy po `BaseIntegrationSpec` i nie uruchamia
PostgreSQL. Nie potrzebujesz Docker Desktop ani działającej aplikacji
Spring Boot.

## 3. Co testują te mocki?

- Pierwszy test sprawdza, że niepoprawna kwota jest odrzucona przed
  jakimkolwiek odczytem lub zapisem repozytorium.
- Drugi sprawdza idempotencję: istniejący wpis `TRANSFER_OUT` powoduje
  wynik `false`, a serwis nie pobiera kont do blokady ani nie zapisuje
  kolejnych danych.
- Trzeci sprawdza decyzję biznesową przy niewystarczającym saldzie:
  przelew kończy się wyjątkiem, nie wywołuje `save` i nie modyfikuje
  sald obiektów przekazanych przez mock.

To testy zachowania serwisu przy zadanych odpowiedziach zależności.
Nie weryfikują zapytań SQL, mapowania JPA, prawdziwego rollbacku,
blokad PostgreSQL ani działania `@Transactional`.

## Eksperymenty

1. Zmień `BigDecimal.ZERO` na `null`. Sprawdź, który wyjątek zwraca
   `validateAmount` i czy repozytoria nadal pozostają nietknięte.
2. W teście duplikatu zmień zwracane `Optional.of(existing)` na
   `Optional.empty()`. Jak zmienia się dalsza interakcja z repozytoriami?
   Nie usuwaj istniejących oczekiwań bez zastanowienia.
3. Dodaj test, który odrzuca transfer na to samo konto. Ustal, czy
   wyszukanie wpisu idempotencji powinno zostać wywołane.
4. Dodaj test błędu waluty. Przygotuj konta z różnymi walutami,
   zamockuj wynik blokującego zapytania i potwierdź brak zapisów.
5. Porównaj tę specyfikację z `BankAccountBusinessSpec`. Który test
   jest szybszy i bardziej izolowany, a który daje dowód, że działa
   integracja z PostgreSQL?

## Jak o tym opowiedzieć

Test jednostkowy sprawdza mały fragment zachowania w izolacji. Mock
repozytorium pozwala kontrolować odpowiedzi zależności i weryfikować
interakcje, ale sam w sobie nie dowodzi poprawności SQL ani konfiguracji
Springa.

Warto rozdzielać testy według pytania, na które odpowiadają:

- test jednostkowy: czy serwis podejmuje właściwą decyzję dla danych wejściowych?
- test MVC: czy żądanie i odpowiedź HTTP są poprawne?
- test integracyjny: czy Spring, JPA i prawdziwa baza współpracują poprawnie?

Nie mockuj każdej klasy automatycznie. Jeśli test sprawdza wyłącznie
liczbę wywołań, ale nie zachowanie istotne dla kontraktu, może być
kruchy przy refaktoryzacji.

## Pytania kontrolne

1. Dlaczego ta specyfikacja nie dziedziczy po `BaseIntegrationSpec`?
2. Co oznacza `0 * accountRepository._` w Spocku?
3. Czy `new BankAccountService(...)` uruchamia `@Transactional`?
   Dlaczego?
4. Których problemów nie da się wiarygodnie wykryć tym testem jednostkowym?
5. Kiedy mock jest pomocny, a kiedy lepiej użyć prawdziwej implementacji?

## Do zapamiętania

- Test jednostkowy serwisu może działać bez Spring context i PostgreSQL.
- Mocki kontrolują odpowiedzi repozytoriów i ujawniają niepożądane interakcje.
- Bez proxy Springa adnotacja `@Transactional` nie jest tu testowana.
- Testy jednostkowe, MVC i integracyjne uzupełniają się — nie zastępują.
