# Lab 32 — Groovy: transformacje AST i `@Canonical`

## Dlaczego ten temat?

Groovy pozwala adnotacjom zmieniać kod podczas kompilacji. Te zmiany są
nazywane **transformacjami AST** — kompilator modyfikuje abstrakcyjne
drzewo składniowe klasy, zanim powstanie kod wykonywalny.

W tym labie poznasz transformację `@Canonical`, która może wygenerować
przydatne metody klasy:

- konstruktor przyjmujący wszystkie właściwości,
- `equals()` i `hashCode()`,
- `toString()`.

Porównasz zachowanie klasy z adnotacją i bez niej oraz sprawdzisz
kontrakt generowanych metod przez Spocka. Zobaczysz też, dlaczego
`@Canonical` nie oznacza automatycznie, że obiekt jest niemutowalny.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. Czym jest transformacja AST?

AST (*Abstract Syntax Tree*) to struktura reprezentująca kod po
parsowaniu. Transformacja AST może zmodyfikować tę strukturę podczas
kompilacji — na przykład dodać metody albo sprawdzenia, których nie
napisałeś ręcznie.

Transformacja nie jest mechanizmem działającym przy każdym wywołaniu
metody w runtime. Kod jest przetwarzany podczas kompilowania klasy.

Groovy oferuje między innymi wbudowane transformacje:

| Adnotacja | Typowe działanie |
|---|---|
| `@Canonical` | konstruktor pozycyjny, `equals`/`hashCode`, `toString` |
| `@Immutable` | model z ograniczeniami mutowalności i metodami wartości |
| `@ToString` | generowanie `toString()` |
| `@EqualsAndHashCode` | generowanie `equals()` i `hashCode()` |
| `@TupleConstructor` | generowanie konstruktora pozycyjnego |

W tym labie skupiamy się na `@Canonical`. Nie dodawaj ręcznie metod,
które ta transformacja ma wygenerować — obserwujemy efekt adnotacji.

## 2. Model transakcji z `@Canonical`

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\ast
```

Utwórz plik `TransactionKind.groovy`:

```groovy
package com.delta.bank.lab.groovy.ast

enum TransactionKind {
    DEPOSIT,
    WITHDRAWAL
}
```

Utwórz plik `TransactionRecord.groovy`:

```groovy
package com.delta.bank.lab.groovy.ast

import groovy.transform.Canonical

import java.math.BigDecimal

@Canonical
class TransactionRecord {
    String accountNumber
    TransactionKind kind
    BigDecimal amount
}
```

`@Canonical` generuje metody na podstawie właściwości klasy. Dzięki temu
można tworzyć obiekty pozycyjnie:

```groovy
def tx = new TransactionRecord(
        "PLN-1001",
        TransactionKind.DEPOSIT,
        new BigDecimal("25.00")
)
```

Wygenerowane `toString()` zawiera nazwę klasy i wartości właściwości.
Nie traktuj formatu `toString()` jako stabilnego formatu API lub
serializacji — jego celem jest przede wszystkim wygodna reprezentacja
obiektu.

## 3. Równość wartości

Dzięki `@Canonical` obiekty z tymi samymi wartościami właściwości są
porównywane przez wygenerowane `equals()`:

```groovy
def first = new TransactionRecord(
        "PLN-1001",
        TransactionKind.DEPOSIT,
        new BigDecimal("25.00")
)

def second = new TransactionRecord(
        "PLN-1001",
        TransactionKind.DEPOSIT,
        new BigDecimal("25.00")
)

assert first == second
assert first.hashCode() == second.hashCode()
```

Ważna pułapka: w Groovy operator `==` porównuje liczby według ich
wartości liczbowej, więc:

```groovy
new BigDecimal("25.0") == new BigDecimal("25.00") // true w Groovy
new BigDecimal("25.0").equals(new BigDecimal("25.00")) // false w Javie
new BigDecimal("25.0").compareTo(new BigDecimal("25.00")) == 0 // true
```

Wygenerowane przez `@Canonical` `equals()` używa semantyki porównania
Groovy dla właściwości, dlatego dwa obiekty z kwotami `25.0` i `25.00`
są w tym przykładzie równe. To różni się od bezpośredniego wywołania
`BigDecimal.equals()`.

Jest tu dodatkowa pułapka: wygenerowane `hashCode()` opiera się na
`hashCode()` wartości właściwości. `BigDecimal.hashCode()` uwzględnia
skalę, więc obiekty uznane przez wygenerowane `equals()` za równe mogą
mieć różne kody hash. Nie używaj takiego modelu bezpośrednio jako klucza
`HashMap` ani elementu `HashSet`, jeśli skala kwoty może się różnić.
W systemie pieniężnym wybierz jawny kontrakt, np. normalizuj kwoty do
ustalonej skali przed utworzeniem obiektu albo zaimplementuj `equals()`
i `hashCode()` tak, by opierały się na tej samej reprezentacji.

## 4. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\ast
```

Utwórz w nim `TransactionRecordSpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.ast

import spock.lang.Specification

import java.math.BigDecimal

class TransactionRecordSpec extends Specification {

    def "canonical constructor assigns all properties"() {
        when:
        def transaction = new TransactionRecord(
                "PLN-1001",
                TransactionKind.DEPOSIT,
                new BigDecimal("25.00")
        )

        then:
        transaction.accountNumber == "PLN-1001"
        transaction.kind == TransactionKind.DEPOSIT
        transaction.amount == new BigDecimal("25.00")
    }

    def "canonical equality compares property values"() {
        given:
        def first = transaction("25.00")
        def second = transaction("25.00")

        expect:
        first == second
        first.hashCode() == second.hashCode()
    }

    def "different properties make transactions unequal"() {
        expect:
        transaction("25.00") != new TransactionRecord(
                "PLN-2002",
                TransactionKind.DEPOSIT,
                new BigDecimal("25.00")
        )

        transaction("25.00") != new TransactionRecord(
                "PLN-1001",
                TransactionKind.WITHDRAWAL,
                new BigDecimal("25.00")
        )
    }

    def "toString includes the class and property values"() {
        when:
        def text = transaction("25.00").toString()

        then:
        text.contains("TransactionRecord")
        text.contains("PLN-1001")
        text.contains("DEPOSIT")
        text.contains("25.00")
    }

    def "canonical equality uses Groovy numeric comparison for BigDecimal"() {
        expect:
        transaction("25.0") == transaction("25.00")
        !new BigDecimal("25.0").equals(new BigDecimal("25.00"))
        new BigDecimal("25.0").compareTo(new BigDecimal("25.00")) == 0
    }

    def "canonical equals and hashCode need care with scaled BigDecimal values"() {
        given:
        def oneDecimal = transaction("25.0")
        def twoDecimals = transaction("25.00")

        expect:
        oneDecimal == twoDecimals
        oneDecimal.hashCode() != twoDecimals.hashCode()
    }

    def "canonical object is still mutable by default"() {
        given:
        def transaction = transaction("25.00")

        when:
        transaction.amount = new BigDecimal("30.00")

        then:
        transaction.amount == new BigDecimal("30.00")
    }

    private static TransactionRecord transaction(String amount) {
        new TransactionRecord(
                "PLN-1001",
                TransactionKind.DEPOSIT,
                new BigDecimal(amount)
        )
    }
}
```

Test `toString()` sprawdza jedynie obecność informacji, a nie cały
tekst. Dzięki temu nie wiążemy testu z każdym szczegółem formatu
generowanego przez Groovy.

## 5. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.ast.TransactionRecordSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.ast.TransactionRecordSpec"
```

Test nie wymaga uruchomienia kontenera PostgreSQL.

## 6. Eksperymenty

### Eksperyment A: usuń `@Canonical`

Usuń adnotację i uruchom testy. Sprawdź, które zachowania przestają
działać: konstruktor pozycyjny, równość wartości i przydatne
`toString()`. Groovy nadal zapewnia konstruktor domyślny, ale to nie
ten sam kontrakt co konstruktor wygenerowany przez `@Canonical`.

Przywróć adnotację.

### Eksperyment B: zmień skalę kwoty

Utwórz dwa obiekty z kwotami `"25.0"` i `"25.00"`. Porównaj równość
obiektów Groovy, metodę Javy `BigDecimal.equals()` i porównanie liczbowe:

```groovy
first == second
first.amount.equals(second.amount)
first.amount.compareTo(second.amount) == 0
```

Wyniki pokazują różnicę między Groovy `==` a `BigDecimal.equals()`.
Sprawdź też `first.hashCode()` i `second.hashCode()`: wyjaśnij, dlaczego
różne kody hash dla równych obiektów są problemem dla `HashMap` i `HashSet`.

### Eksperyment C: użyj obiektów jako kluczy mapy

Dodaj oba obiekty do `Set` lub użyj ich jako kluczy mapy. Sprawdź,
jak `equals()` i `hashCode()` wpływają na wynik, a następnie powtórz
eksperyment dla równych obiektów o identycznej skali kwoty.

### Eksperyment D: dodaj `@Immutable`

Na osobnej, tymczasowej klasie sprawdź zachowanie `@Immutable` i
porównaj je z `@Canonical`. Nie nakładaj obu adnotacji na model
z tego zadania bez sprawdzenia ich wspólnego kontraktu.

## 7. Ograniczenia i dobre praktyki

- `@Canonical` generuje metody wartości, ale nie waliduje wymagań
  domenowych.
- Obiekt z `@Canonical` nie jest automatycznie niemutowalny.
- Wygenerowane `equals()` korzysta z semantyki równości Groovy właściwości;
  dla liczb może ona różnić się od wywołania ich metody `equals()`.
- Uważaj na wygenerowany `hashCode()` dla właściwości `BigDecimal`: jego
  kod uwzględnia skalę, mimo że Groovy może uznać liczby o różnej skali za
  równe. Taka para metod nie jest bezpieczna dla kluczy map i zbiorów.
- Uważaj na zmienne obiekty w `equals()`/`hashCode()`, jeśli obiekt jest
  używany w `Set` lub jako klucz mapy, a jego pola mogą się zmienić.
- Nie używaj wygenerowanego `toString()` jako kontraktu publicznego API
  ani formatu do zapisu danych.
- Jeśli potrzebujesz walidacji, dodaj ją jawnie w konstruktorze albo
  użyj dedykowanej fabryki.

## Pytania kontrolne

1. Co oznacza transformacja AST?
2. Jakie metody i konstruktor może wygenerować `@Canonical`?
3. Czy `@Canonical` czyni obiekt niemutowalnym?
4. Dlaczego Groovy `==` może dać inny wynik niż
   `BigDecimal.equals()` dla `"25.0"` i `"25.00"`?
5. Dlaczego różne `hashCode()` dla obiektów uznanych przez `equals()` za
   równe łamią kontrakt map i zbiorów?
6. Dlaczego zmiana pola używanego w `hashCode()` jest ryzykowna, gdy
   obiekt znajduje się w `HashSet`?
7. Dlaczego nie należy traktować domyślnego `toString()` jako formatu API?
8. Kiedy warto dodać jawny konstruktor lub fabrykę zamiast polegać
   wyłącznie na `@Canonical`?

Do zapamiętania:

```text
AST transformation → zmiana struktury klasy podczas kompilacji
@Canonical         → konstruktor pozycyjny, equals/hashCode, toString
@Canonical         ≠ automatyczna walidacja ani niemutowalność
Groovy == dla liczb → porównuje wartość liczbową
BigDecimal.equals  → uwzględnia skalę
BigDecimal.compareTo → porównuje wartość liczbową
equals/hashCode    → muszą używać zgodnej reguły równości
```
