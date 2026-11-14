# Lab 38 — Operatory Groovy: zakresy, spread, przeciążanie operatorów

## Dlaczego ten temat?

Po adnotacjach AST (Lab 32, 37) czas na składnię, którą Groovy dodaje
do codziennego kodu. Poznasz:

- zakresy (`1..5`, `1..<5`) i ich użycie w pętlach oraz indeksowaniu,
- operator spread `*.` oraz `*` dla list,
- operator `<=>` (compareTo),
- przeciążanie operatorów przez metody `plus`, `minus`, `multiply`, `negative`,
- `in` (`isCase`) i operator `as`.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. Klasa `Money` z przeciążonymi operatorami

Plik: `src\main\groovy\com\delta\bank\lab\groovy\operators\Money.groovy`

```groovy
package com.delta.bank.lab.groovy.operators

class Money implements Comparable<Money> {

    final BigDecimal amount
    final String currency

    Money(BigDecimal amount, String currency) {
        this.amount = amount
        this.currency = currency
    }

    Money plus(Money other) {
        requireSameCurrency(other)
        new Money(amount + other.amount, currency)
    }

    Money minus(Money other) {
        requireSameCurrency(other)
        new Money(amount - other.amount, currency)
    }

    Money multiply(Number factor) {
        new Money(amount * factor, currency)
    }

    Money negative() {
        new Money(-amount, currency)
    }

    @Override
    int compareTo(Money other) {
        requireSameCurrency(other)
        amount <=> other.amount
    }

    private void requireSameCurrency(Money other) {
        if (currency != other.currency) {
            throw new IllegalArgumentException("Currency mismatch: $currency vs $other.currency")
        }
    }

    @Override
    String toString() { "$amount $currency" }
}
```

## 2. Specyfikacja Spocka

Plik: `src\test\groovy\com\delta\bank\lab\groovy\operators\OperatorsSpec.groovy`

```groovy
package com.delta.bank.lab.groovy.operators

import spock.lang.Specification

class OperatorsSpec extends Specification {

    def "ranges are inclusive or exclusive"() {
        expect:
        (1..5).toList() == [1, 2, 3, 4, 5]
        (1..<5).toList() == [1, 2, 3, 4]
        (5..1).toList() == [5, 4, 3, 2, 1]
        ('a'..'d').toList() == ['a', 'b', 'c', 'd']
    }

    def "ranges slice lists and strings"() {
        given:
        def list = [10, 20, 30, 40, 50]

        expect:
        list[1..3] == [20, 30, 40]
        list[-2..-1] == [40, 50]
        "statement"[0..3] == "stat"
    }

    def "spread operator calls a property on every element"() {
        given:
        def owners = [[name: "Ann"], [name: "Bob"]]

        expect:
        owners*.name == ["Ann", "Bob"]
        [1, 2, 3]*.multiply(2) == [2, 4, 6]
    }

    def "spread expands a list into arguments"() {
        given:
        def add = { a, b, c -> a + b + c }

        expect:
        add(*[1, 2, 3]) == 6
        [0, *[1, 2], 3] == [0, 1, 2, 3]
    }

    def "spaceship compares"() {
        expect:
        (1 <=> 2) == -1
        (2 <=> 2) == 0
        ("b" <=> "a") == 1
    }

    def "operators map to methods"() {
        given:
        def a = new Money(new BigDecimal("10.00"), "PLN")
        def b = new Money(new BigDecimal("2.50"), "PLN")

        expect:
        (a + b).amount == new BigDecimal("12.50")
        (a - b).amount == new BigDecimal("7.50")
        (b * 2).amount == new BigDecimal("5.00")
        (-a).amount == new BigDecimal("-10.00")
        a > b
        b < a
    }

    def "different currencies are rejected"() {
        when:
        new Money(1.0, "PLN") + new Money(1.0, "EUR")

        then:
        thrown(IllegalArgumentException)
    }

    def "in uses isCase and as converts"() {
        expect:
        3 in (1..5)
        !(9 in (1..5))
        "12" as Integer == 12
        [1, 2, 2] as Set == [1, 2] as Set
    }
}
```

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.operators.OperatorsSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.operators.OperatorsSpec"
```

## Eksperymenty

1. Dodaj metodę `div(Number)` i użyj operatora `/`.
2. Dodaj `getAt(int)` i `putAt(int, Object)` do klasy i użyj `obj[0]`.
3. Zamień `Money` na `Money` z `@Canonical` — co się dzieje z `==` przy `compareTo`? (Groovy `==` używa `compareTo` dla `Comparable`!)
4. Użyj `list.sum()` na liście `Money` — czego brakuje? (podpowiedź: `plus` i wartość startowa).
5. Zrób `(1..<1).toList()` — jaki wynik?

## Pytania kontrolne

1. Jaka metoda stoi za `a + b`, `-a`, `a[0]`?
2. Czym różni się `1..5` od `1..<5`?
3. Co robi `*.` na liście zawierającej `null`? (a czym jest `?.`?)
4. Dlaczego `==` w Groovy dla `Comparable` używa `compareTo`?

## Do zapamiętania

- Każdy operator Groovy to wywołanie metody o ustalonej nazwie.
- Zakresy są obiektami (`Range`), a nie tylko składnią pętli.
- `*.` mapuje właściwość/metodę na wszystkie elementy kolekcji.
- `<=>` to `compareTo`; `==` dla `Comparable` korzysta z `compareTo`.
