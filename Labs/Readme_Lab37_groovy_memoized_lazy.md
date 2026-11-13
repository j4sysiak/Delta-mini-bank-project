# Lab 37 — `@Memoized`, `@Lazy` i leniwe obliczenia w Groovy

## Dlaczego ten temat?

W Labie 32 poznałeś transformacje AST (`@Canonical`). Teraz kolejne dwie,
które rozwiązują częsty problem: kosztowne obliczenie nie powinno się
powtarzać ani wykonywać zawczasu.

Poznasz:

- `@Memoized` — cache wyniku metody dla tych samych argumentów,
- `@Lazy` — pole inicjalizowane przy pierwszym odczycie,
- ograniczenia: stan zmienny, rozmiar cache (`maxCacheSize`),
- jak w Spocku policzyć wywołania (licznik zamiast mocka).

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. Klasa z kosztownym obliczeniem

Plik: `src\main\groovy\com\delta\bank\lab\groovy\lazy\FeeCalculator.groovy`

```groovy
package com.delta.bank.lab.groovy.lazy

import groovy.transform.Memoized

class FeeCalculator {

    int calls = 0

    @Memoized
    BigDecimal feeFor(BigDecimal amount) {
        calls++
        amount.multiply(new BigDecimal("0.01")).setScale(2, java.math.RoundingMode.HALF_UP)
    }

    @Memoized(maxCacheSize = 2)
    String label(String code) {
        calls++
        "FEE-" + code
    }
}
```

## 2. `@Lazy`

Plik: `src\main\groovy\com\delta\bank\lab\groovy\lazy\StatementCache.groovy`

```groovy
package com.delta.bank.lab.groovy.lazy

class StatementCache {

    int loads = 0

    @Lazy
    List<String> lines = load()

    private List<String> load() {
        loads++
        ["line-1", "line-2"]
    }
}
```

Uwaga: `@Lazy` generuje pole `$lines` i getter, który inicjalizuje je
przy pierwszym odczycie `lines`.

## 3. Specyfikacja Spocka

Plik: `src\test\groovy\com\delta\bank\lab\groovy\lazy\LazySpec.groovy`

```groovy
package com.delta.bank.lab.groovy.lazy

import spock.lang.Specification

class LazySpec extends Specification {

    def "memoized returns the same result and computes once per argument"() {
        given:
        def calc = new FeeCalculator()

        when:
        def first = calc.feeFor(new BigDecimal("100.00"))
        def second = calc.feeFor(new BigDecimal("100.00"))

        then:
        first == new BigDecimal("1.00")
        second.is(first)
        calc.calls == 1
    }

    def "different arguments are computed separately"() {
        given:
        def calc = new FeeCalculator()

        when:
        calc.feeFor(new BigDecimal("100.00"))
        calc.feeFor(new BigDecimal("200.00"))

        then:
        calc.calls == 2
    }

    def "maxCacheSize evicts old entries"() {
        given:
        def calc = new FeeCalculator()

        when:
        calc.label("A")
        calc.label("B")
        calc.label("C")
        calc.label("A")

        then:
        calc.calls == 4
    }

    def "lazy field loads only on first read"() {
        given:
        def cache = new StatementCache()

        expect:
        cache.loads == 0

        when:
        def first = cache.lines
        def second = cache.lines

        then:
        first.is(second)
        cache.loads == 1
    }
}
```

Jeśli test `maxCacheSize` zachowa się inaczej (eksmisja zależy od
implementacji cache), sprawdź w eksperymentach, ile wynosi `calls`,
i wyjaśnij dlaczego.

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.lazy.LazySpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.lazy.LazySpec"
```

## Eksperymenty

1. Dodaj do `feeFor` argument `String currency` i sprawdź, że cache działa per para argumentów.
2. Zmień `new BigDecimal("100.00")` na `100.0`. Czy to ten sam klucz cache? (podpowiedź: skala w `equals`/`hashCode`, Lab 32).
3. Dodaj do `FeeCalculator` pole zmienne (np. stawkę) i pokaż, jak `@Memoized` zwraca nieaktualny wynik.
4. Wypróbuj `@Memoized(protectedCacheSize = 1)` i opisz różnicę.
5. Rzuć wyjątek w `feeFor`. Czy wynik-wyjątek jest cache'owany?

## Pytania kontrolne

1. Dlaczego `@Memoized` jest bezpieczne tylko dla metod deterministycznych?
2. Co robi `maxCacheSize`?
3. Kiedy `@Lazy` jest lepsze od inicjalizacji w konstruktorze?
4. Dlaczego `@Memoized` na metodzie z `BigDecimal` może zaskoczyć skalą?

## Do zapamiętania

- `@Memoized` = cache wyniku per argumenty; metoda powinna być czysta.
- `maxCacheSize` ogranicza pamięć.
- `@Lazy` odracza koszt do pierwszego użycia.
- Licznik wywołań w teście wystarczy do sprawdzenia cache — bez mocka.
