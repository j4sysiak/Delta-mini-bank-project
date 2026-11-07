# Lab 27 — Groovy: `@CompileStatic` i kontrola typów

## Dlaczego ten temat?

Lab 25 pokazał closures i kolekcje Groovy, a Lab 26 — safe navigation,
Elvis i Groovy Truth. W tym labie zobaczysz, jak zachować zwięzłość
Groovy, a jednocześnie wcześniej wykrywać część błędów typów.

Porównamy:

- dynamiczne zachowanie Groovy, które jest domyślne,
- `@TypeChecked`, które włącza kontrolę typów podczas kompilacji,
- `@CompileStatic`, które kontroluje typy i stosuje statyczne wywołania.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. `def` nie oznacza, że typ obiektu zmienia się w czasie

W Groovy można napisać:

```groovy
def amount = new BigDecimal("10.00")
```

`def` oznacza, że nie zapisujesz jawnie typu zmiennej. Nie zmienia jednak
typu utworzonego obiektu — `amount` nadal wskazuje na `BigDecimal`.

W zwykłym, dynamicznym kodzie Groovy kompilator może odłożyć sprawdzenie
dostępności metody do czasu wykonania:

```groovy
def formatAmount(value) {
    value.toPlainString()
}
```

Wywołanie z `BigDecimal` zadziała. Przekazanie `String` spowoduje błąd
dopiero w czasie wykonania, ponieważ `String` nie ma metody
`toPlainString()`.

## 2. `@TypeChecked` a `@CompileStatic`

`@TypeChecked` prosi kompilator Groovy o sprawdzenie typów i wywołań
metod w kompilowanym zakresie:

```groovy
import groovy.transform.TypeChecked

@TypeChecked
class CheckedReport {
    String describe(String owner) {
        owner.toUpperCase()
    }
}
```

Niepoprawne wywołanie, takie jak `owner.toUppercase()`, zostanie
zasygnalizowane podczas kompilacji zamiast dopiero po uruchomieniu
tej ścieżki.

`@CompileStatic` wykonuje kontrolę typów i generuje kod z wywołaniami
statycznymi, zbliżonymi semantycznie do Javy:

```groovy
import groovy.transform.CompileStatic

@CompileStatic
class StaticReport {
    String describe(String owner) {
        owner.toUpperCase()
    }
}
```

W uproszczeniu:

| Tryb | Sprawdzenie typów | Sposób wywołań |
|---|---|---|
| domyślny Groovy | część problemów ujawnia się w runtime | dynamiczny |
| `@TypeChecked` | podczas kompilacji | nadal dynamiczny |
| `@CompileStatic` | podczas kompilacji | statyczny |

Statyczna kompilacja nie jest automatyczną gwarancją szybszego programu.
Jej główną korzyścią w tym ćwiczeniu jest wcześniejsza kontrola typów
i bardziej przewidywalne wywołania. Może też ograniczyć użycie dynamicznych
funkcji Groovy w objętym nią kodzie.

Adnotację można zastosować do klasy albo metody. Nie dodawaj jej
mechanicznie wszędzie: wybierz ją tam, gdzie zależy Ci na statycznej
kontroli i nie potrzebujesz dynamicznych możliwości Groovy.

## 3. Model szkoleniowy

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\staticcompilation
```

Utwórz plik `TransactionKind.groovy`:

```groovy
package com.delta.bank.lab.groovy.staticcompilation

enum TransactionKind {
    DEPOSIT,
    WITHDRAWAL
}
```

Utwórz plik `BankTransaction.groovy`:

```groovy
package com.delta.bank.lab.groovy.staticcompilation

import groovy.transform.CompileStatic

import java.math.BigDecimal
import java.util.Objects

@CompileStatic
class BankTransaction {
    final String accountNumber
    final TransactionKind kind
    final BigDecimal amount

    BankTransaction(
            String accountNumber,
            TransactionKind kind,
            BigDecimal amount
    ) {
        this.accountNumber = Objects.requireNonNull(
                accountNumber,
                "accountNumber must not be null"
        )
        this.kind = Objects.requireNonNull(kind, "kind must not be null")
        this.amount = Objects.requireNonNull(amount, "amount must not be null")

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank")
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero")
        }
    }
}
```

W Groovy pola mają akcesory właściwości, więc można odczytać na przykład
`transaction.amount`. Adnotacja `@CompileStatic` pozwala kompilatorowi
sprawdzić również te odwołania.

## 4. Raport z typowanymi closures

Utwórz `TransactionReport.groovy` w tym samym katalogu:

```groovy
package com.delta.bank.lab.groovy.staticcompilation

import groovy.transform.CompileStatic

import java.math.BigDecimal
import java.util.Objects

@CompileStatic
class TransactionReport {

    private final List<BankTransaction> transactions

    TransactionReport(List<BankTransaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null")

        if (transactions.any { BankTransaction tx -> tx == null }) {
            throw new IllegalArgumentException("transactions must not contain null")
        }

        this.transactions = List.copyOf(transactions)
    }

    List<BigDecimal> depositAmounts() {
        return transactions
                .findAll { BankTransaction tx ->
                    tx.kind == TransactionKind.DEPOSIT
                }
                .collect { BankTransaction tx ->
                    tx.amount
                }
    }

    BigDecimal totalDeposits() {
        BigDecimal total = BigDecimal.ZERO

        for (BankTransaction tx : transactions) {
            if (tx.kind == TransactionKind.DEPOSIT) {
                total = total.add(tx.amount)
            }
        }

        return total
    }
}
```

Closures mają jawnie określony typ parametru `BankTransaction`. Dzięki
temu statyczny kompilator może sprawdzać odwołania takie jak `tx.kind`
i `tx.amount`.

Lista jest kopiowana w konstruktorze. Dzięki temu późniejsza zmiana listy
wejściowej nie zmienia danych raportu.

## 5. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\staticcompilation
```

Utwórz w nim `TransactionReportSpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.staticcompilation

import spock.lang.Specification

import java.math.BigDecimal

class TransactionReportSpec extends Specification {

    def "returns amounts for deposits only"() {
        expect:
        report().depositAmounts() == [
                new BigDecimal("100.00"),
                new BigDecimal("25.50")
        ]
    }

    def "sums deposit amounts"() {
        expect:
        report().totalDeposits().compareTo(new BigDecimal("125.50")) == 0
    }

    def "returns zero when there are no deposits"() {
        given:
        def report = new TransactionReport([
                transaction(TransactionKind.WITHDRAWAL, "10.00")
        ])

        expect:
        report.totalDeposits().compareTo(BigDecimal.ZERO) == 0
    }

    def "does not retain the input list"() {
        given:
        def transactions = new ArrayList<BankTransaction>()
        transactions.add(transaction(TransactionKind.DEPOSIT, "10.00"))
        def report = new TransactionReport(transactions)

        when:
        transactions.add(transaction(TransactionKind.DEPOSIT, "20.00"))

        then:
        report.depositAmounts() == [new BigDecimal("10.00")]
    }

    def "rejects a null transaction"() {
        given:
        def transactions = new ArrayList<BankTransaction>()
        transactions.add(null)

        when:
        new TransactionReport(transactions)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "transactions must not contain null"
    }

    private static TransactionReport report() {
        new TransactionReport([
                transaction(TransactionKind.DEPOSIT, "100.00"),
                transaction(TransactionKind.WITHDRAWAL, "10.00"),
                transaction(TransactionKind.DEPOSIT, "25.50")
        ])
    }

    private static BankTransaction transaction(
            TransactionKind kind,
            String amount
    ) {
        new BankTransaction(
                "PLN-1001",
                kind,
                new BigDecimal(amount)
        )
    }
}
```

Spec Spocka nie ma `@CompileStatic`, bo DSL Spocka i dynamiczna składnia
Groovy są użyteczne w testach. W ćwiczeniu statycznie kompilujemy klasy
produkcyjne laboratorium, a test pozostawiamy czytelny i idiomatyczny
dla Spocka.

## 6. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.staticcompilation.TransactionReportSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.staticcompilation.TransactionReportSpec"
```

Test nie wymaga uruchomienia kontenera PostgreSQL.

## 7. Eksperymenty

### Eksperyment A: błąd wykrywany przy kompilacji

W `depositAmounts()` zmień tymczasowo:

```groovy
tx.amount
```

na:

```groovy
tx.amunt
```

Uruchom test. Zwróć uwagę, że `@CompileStatic` powoduje błąd kompilacji
zanim test zostanie wykonany. Przywróć poprawną nazwę właściwości.

### Eksperyment B: usuń `@CompileStatic`

Usuń adnotację z `TransactionReport`, ale pozostaw literówkę `tx.amunt`.
Uruchom test ponownie i porównaj moment oraz rodzaj błędu. Przywróć
poprawny kod i adnotację.

### Eksperyment C: zmień typ parametru closure

Zmień typ `BankTransaction tx` na `String tx` w jednej z closures.
Sprawdź, czy kompilator zgłasza problem przy odwołaniu do `tx.kind`
lub `tx.amount`. Przywróć prawidłowy typ.

## 8. Ograniczenia i wybór trybu

Statyczna kontrola może odrzucić konstrukcje, które domyślny Groovy
obsługuje dynamicznie, na przykład część wywołań zależnych od
metaprogramowania. Jeśli konkretny fragment musi pozostać dynamiczny,
Groovy udostępnia `@CompileDynamic` dla odpowiedniego zakresu. Nie używaj
go do ukrywania błędów typów — stosuj tylko wtedy, gdy rzeczywiście
potrzebujesz dynamicznego zachowania.

Praktyczna reguła:

```text
dynamiczny Groovy → wygodny DSL i metaprogramowanie
@TypeChecked      → kontrola typów, dynamiczne wywołania
@CompileStatic    → kontrola typów i statyczne wywołania
```

## Pytania kontrolne

1. Czy `def` oznacza, że obiekt może zmienić swój typ?
2. Kiedy dynamiczny kod Groovy może ujawnić brak metody?
3. Co sprawdza `@TypeChecked`?
4. Czym `@CompileStatic` różni się od `@TypeChecked`?
5. Dlaczego closure w `TransactionReport` jawnie deklaruje typ parametru?
6. Czy `@CompileStatic` gwarantuje szybsze działanie programu?
7. Jaki rodzaj kodu może wymagać dynamicznego trybu Groovy?
8. Dlaczego zostawiliśmy spec Spocka bez `@CompileStatic`?

Do zapamiętania:

```text
def              → brak jawnej deklaracji typu, nie zmiana typu obiektu
domyślny Groovy  → dynamiczne wywołania
@TypeChecked     → sprawdzanie typów podczas kompilacji
@CompileStatic   → sprawdzanie typów i statyczne wywołania
typowana closure → czytelniejsza intencja i możliwość statycznej kontroli
```
