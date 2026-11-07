# Lab 25 — Groovy: closures i kolekcje

## Dlaczego ten temat?

Dotychczasowe laby obejmowały wiele funkcji Java 21, podstawy Spocka,
tabele danych i mocki. Brakuje osobnego ćwiczenia z samego Groovy:
closures oraz metod kolekcji z Groovy Development Kit (GDK).

W tym labie napiszesz mały analizator transakcji jako klasę Groovy i
przetestujesz ją w Spocku. Zobaczysz, jak closures współpracują z
`find`, `findAll`, `collect`, `groupBy` i `inject`, oraz porównasz ten
sposób pracy ze streamami z Labu 14.

Nie zmieniaj kodu produkcyjnego mini-banku ani konfiguracji Gradle.
Dodaj tylko wskazane pliki szkoleniowe. Lab nie wymaga Springa, bazy
danych, sieci ani preview Java.

## 1. Czym jest closure?

Closure to blok kodu, który można przekazać do metody, zapisać w zmiennej
i wykonać później.

```groovy
def describe = { transaction ->
    "${transaction.accountNumber}: ${transaction.kind}"
}
```

W tym przykładzie `transaction` jest jawnym parametrem closure. Jeśli
closure ma dokładnie jeden parametr, Groovy pozwala pominąć jego nazwę
i odwołać się do niego przez `it`:

```groovy
def describe = { "${it.accountNumber}: ${it.kind}" }
```

`it` jest skrótem, a nie specjalnym typem. Przy bardziej złożonej logice
preferuj nazwany parametr — łatwiej wtedy zrozumieć kod.

Closure może też mieć kilka parametrów:

```groovy
def addAmounts = { BigDecimal left, BigDecimal right ->
    left.add(right)
}
```

## 2. Metody kolekcji z GDK

Groovy dodaje do kolekcji metody przyjmujące closures:

| Metoda | Co robi | Wynik |
|---|---|---|
| `each { ... }` | wykonuje akcję dla każdego elementu | oryginalna kolekcja |
| `find { ... }` | znajduje pierwszy pasujący element | element albo `null` |
| `findAll { ... }` | wybiera wszystkie pasujące elementy | nowa lista |
| `collect { ... }` | przekształca elementy | nowa lista |
| `groupBy { ... }` | grupuje elementy według klucza | mapa klucz → lista |
| `inject(seed) { ... }` | scala elementy do jednej wartości | wynik akumulacji |

Przykłady:

```groovy
def deposits = transactions.findAll { tx ->
    tx.kind == TransactionKind.DEPOSIT
}

def accountNumbers = transactions.collect { tx ->
    tx.accountNumber
}

def byKind = transactions.groupBy { tx ->
    tx.kind
}
```

`find` zwraca `null`, jeśli niczego nie znajdzie. W tym labie nie
opakowujemy tego wyniku w `Optional`; sprawdź jawnie oba przypadki.

`each` przydaje się do działań ubocznych, na przykład wypisania każdego
elementu. Nie używaj go do budowania nowej listy — do tego służy `collect`.
Gdy chcesz otrzymać nowy wynik, preferuj operacje, które zwracają ten
wynik, zamiast modyfikować współdzielony stan wewnątrz closure.

## 3. Model szkoleniowy

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\closures
```

W tym katalogu utwórz plik `TransactionKind.groovy`:

```groovy
package com.delta.bank.lab.groovy.closures

enum TransactionKind {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}
```

Utwórz plik `BankTransaction.groovy`:

```groovy
package com.delta.bank.lab.groovy.closures

import java.math.BigDecimal
import java.util.Objects

class BankTransaction {
    final String accountNumber
    final TransactionKind kind
    final BigDecimal amount

    BankTransaction(
            String accountNumber,
            TransactionKind kind,
            BigDecimal amount
    ) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null")
        Objects.requireNonNull(kind, "kind must not be null")
        Objects.requireNonNull(amount, "amount must not be null")

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank")
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero")
        }

        this.accountNumber = accountNumber
        this.kind = kind
        this.amount = amount
    }
}
```

Pola `final` nie mają setterów, a Groovy udostępnia do nich akcesory,
więc w closure możesz użyć czytelnego zapisu właściwości, na przykład
`tx.accountNumber`. Nie używamy tu `@Immutable`, aby walidacja konstruktora
była jawna i łatwa do prześledzenia.

## 4. Analizator transakcji

Utwórz plik `BankTransactionAnalyzer.groovy` w tym samym katalogu:

```groovy
package com.delta.bank.lab.groovy.closures

import java.math.BigDecimal
import java.util.Objects

class BankTransactionAnalyzer {

    private final List<BankTransaction> transactions

    BankTransactionAnalyzer(List<BankTransaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null")

        if (transactions.any { tx -> tx == null }) {
            throw new IllegalArgumentException("transactions must not contain null")
        }

        this.transactions = List.copyOf(transactions)
    }

    List<BankTransaction> findByKind(TransactionKind kind) {
        Objects.requireNonNull(kind, "kind must not be null")

        transactions.findAll { tx ->
            tx.kind == kind
        }
    }

    List<String> descriptions() {
        transactions.collect { tx ->
            "${tx.accountNumber}: ${tx.kind} ${tx.amount.toPlainString()}"
        }
    }

    BankTransaction firstAtLeast(TransactionKind kind, BigDecimal minimum) {
        Objects.requireNonNull(kind, "kind must not be null")
        Objects.requireNonNull(minimum, "minimum must not be null")

        transactions.find { tx ->
            tx.kind == kind && tx.amount.compareTo(minimum) >= 0
        }
    }

    Map<TransactionKind, List<BankTransaction>> groupByKind() {
        transactions.groupBy { tx ->
            tx.kind
        }
    }

    BigDecimal totalFor(TransactionKind kind) {
        Objects.requireNonNull(kind, "kind must not be null")

        transactions
                .findAll { tx -> tx.kind == kind }
                .inject(BigDecimal.ZERO) { BigDecimal total, BankTransaction tx ->
                    total.add(tx.amount)
                }
    }
}
```

Zwróć uwagę na różne role closures:

- `findAll` zwraca wybrane transakcje,
- `collect` mapuje transakcję na opis,
- `find` zwraca pierwsze dopasowanie albo `null`,
- `groupBy` tworzy mapę grup,
- `inject` przenosi akumulator `total` przez kolejne transakcje.

`BigDecimal` porównujemy przez `compareTo`, a sumę liczymy przez
`add(...)`. Nie używamy `double` do kwot.

## 5. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\closures
```

Utwórz w nim `BankTransactionAnalyzerSpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.closures

import spock.lang.Specification

import java.math.BigDecimal

class BankTransactionAnalyzerSpec extends Specification {

    def "findAll wybiera wszystkie transakcje danego rodzaju"() {
        expect:
        analyzer().findByKind(TransactionKind.DEPOSIT)*.accountNumber ==
                ["PLN-1001", "PLN-1002"]
    }

    def "collect tworzy opis każdej transakcji"() {
        expect:
        analyzer().descriptions() == [
                "PLN-1001: DEPOSIT 100.00",
                "PLN-1001: WITHDRAWAL 25.00",
                "PLN-1002: DEPOSIT 10.00",
                "PLN-1001: TRANSFER 40.00"
        ]
    }

    def "find zwraca pierwsze pasujące dopasowanie"() {
        when:
        def result = analyzer().firstAtLeast(
                TransactionKind.DEPOSIT,
                new BigDecimal("50.00")
        )

        then:
        result.accountNumber == "PLN-1001"
        result.amount.compareTo(new BigDecimal("100.00")) == 0
    }

    def "find zwraca null, gdy nie ma pasującej transakcji"() {
        expect:
        analyzer().firstAtLeast(
                TransactionKind.WITHDRAWAL,
                new BigDecimal("50.00")
        ) == null
    }

    def "groupBy tworzy osobną grupę dla każdego rodzaju"() {
        when:
        def groups = analyzer().groupByKind()

        then:
        groups.keySet() == ([
                TransactionKind.DEPOSIT,
                TransactionKind.WITHDRAWAL,
                TransactionKind.TRANSFER
        ] as Set)
        groups[TransactionKind.DEPOSIT]*.accountNumber == ["PLN-1001", "PLN-1002"]
        groups[TransactionKind.WITHDRAWAL].size() == 1
        groups[TransactionKind.TRANSFER].size() == 1
    }

    def "inject sumuje kwoty dla wybranego rodzaju"() {
        expect:
        analyzer().totalFor(TransactionKind.DEPOSIT)
                .compareTo(new BigDecimal("110.00")) == 0
        analyzer().totalFor(TransactionKind.WITHDRAWAL)
                .compareTo(new BigDecimal("25.00")) == 0
        analyzer().totalFor(TransactionKind.TRANSFER)
                .compareTo(new BigDecimal("40.00")) == 0
    }

    def "inject zwraca zero dla pustej listy"() {
        expect:
        new BankTransactionAnalyzer([])
                .totalFor(TransactionKind.DEPOSIT)
                .compareTo(BigDecimal.ZERO) == 0
    }

    def "rejects a null transaction in the input list"() {
        given:
        def transactions = new ArrayList<BankTransaction>()
        transactions.add(null)

        when:
        new BankTransactionAnalyzer(transactions)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "transactions must not contain null"
    }

    private static BankTransactionAnalyzer analyzer() {
        new BankTransactionAnalyzer([
                transaction("PLN-1001", TransactionKind.DEPOSIT, "100.00"),
                transaction("PLN-1001", TransactionKind.WITHDRAWAL, "25.00"),
                transaction("PLN-1002", TransactionKind.DEPOSIT, "10.00"),
                transaction("PLN-1001", TransactionKind.TRANSFER, "40.00")
        ])
    }

    private static BankTransaction transaction(
            String accountNumber,
            TransactionKind kind,
            String amount
    ) {
        new BankTransaction(accountNumber, kind, new BigDecimal(amount))
    }

}
```

## 6. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.closures.BankTransactionAnalyzerSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.closures.BankTransactionAnalyzerSpec"
```

Test powinien działać bez uruchamiania kontenera PostgreSQL.

## 7. Ćwiczenia

1. Zmień `descriptions()` tak, aby closure korzystało z `it` zamiast
   nazwanego parametru `tx`. Porównaj czytelność obu wersji.
2. Napisz `accountNumbers()` używając `collect` i zwróć listę numerów kont.
3. Napisz `countByKind()` używając `groupBy` i `collectEntries`.
4. Dodaj test sprawdzający, że konstruktor nie zachowuje referencji do
   wejściowej listy: zmodyfikuj listę po utworzeniu analizatora i sprawdź,
   że wynik analizatora się nie zmienił.
5. Zapisz w teście closure w zmiennej, a następnie przekaż ją do `findAll`:

   ```groovy
   def isDeposit = { BankTransaction tx ->
       tx.kind == TransactionKind.DEPOSIT
   }
   ```

6. Wypisz elementy przez `each`, a potem sprawdź, co zwraca `each`.
   Nie używaj `each` do tworzenia listy wynikowej.

## 8. Groovy GDK a Java Stream API

| Groovy GDK | Java Stream API |
|---|---|
| `list.findAll { ... }` | `stream.filter(...).toList()` |
| `list.collect { ... }` | `stream.map(...).toList()` |
| `list.groupBy { ... }` | `stream.collect(groupingBy(...))` |
| `list.inject(seed) { ... }` | `stream.reduce(...)` |
| metody zwykle od razu zwracają kolekcję/mapę | operacje pośrednie tworzą leniwy pipeline |

GDK jest wygodne do pracy z kolekcjami Groovy. Streamy mają własny kontrakt,
między innymi leniwe wykonanie operacji pośrednich i możliwość budowania
pipeline. Nie zakładaj, że te API zachowują się identycznie tylko dlatego,
że nazwy metod są podobne.

## Pytania kontrolne

1. Co oznacza `it` w closure Groovy?
2. Kiedy warto nazwać parametr closure zamiast używać `it`?
3. Jaka jest różnica między `find` a `findAll`?
4. Co zwraca `find`, jeśli nie ma dopasowania?
5. Kiedy użyć `collect`, a kiedy `each`?
6. Jaki typ wyniku zwraca `groupBy`?
7. Jaką rolę pełni wartość początkowa `BigDecimal.ZERO` w `inject`?
8. Czym różni się typowe wykonanie `findAll` w GDK od `filter` w Stream API?
9. Dlaczego w przykładzie do porównania kwot używamy `compareTo`?

Do zapamiętania:

```text
closure       → kod przekazywany jako wartość
it            → domyślna nazwa jedynego parametru closure
find          → pierwsze dopasowanie albo null
findAll       → wszystkie dopasowania
collect       → przekształcenie elementów
groupBy       → grupowanie do mapy
inject        → akumulacja od jawnej wartości początkowej
```
