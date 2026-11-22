# Lab 46 — Flyweight: współdzielenie niezmiennych danych

## Dlaczego ten temat?

W Labach 42–45 poznałeś wzorce strukturalne, m.in. Decorator, Adapter,
Facade, Proxy i Prototype. **Flyweight** pomaga ograniczyć zużycie
pamięci, gdy aplikacja tworzy wiele obiektów zawierających powtarzające
się dane.

Zbudujesz renderer wyciągu bankowego. Każda transakcja ma własne dane,
takie jak opis i kwota, ale wiele transakcji może współdzielić ten sam
niezmienny styl dla typu operacji. Wzorzec rozdziela:

- **stan wewnętrzny (intrinsic)** — współdzielony styl typu transakcji,
- **stan zewnętrzny (extrinsic)** — dane konkretnej transakcji, przekazane
  przy renderowaniu.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj istniejącego kodu
ani konfiguracji Gradle. Ćwiczenie działa bez Springa, bazy i sieci.

## 1. Typ transakcji i niezmienny Flyweight

Plik: `src\main\java\com\delta\bank\lab\patterns\flyweight\TransactionType.java`

```java
package com.delta.bank.lab.patterns.flyweight;

public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}
```

Plik: `src\main\java\com\delta\bank\lab\patterns\flyweight\TransactionStyle.java`

```java
package com.delta.bank.lab.patterns.flyweight;

import java.util.Objects;

public record TransactionStyle(String label, String marker) {

    public TransactionStyle {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(marker, "marker");
    }

    public String render(String description, String amount) {
        return marker + " " + label + ": " + description + " (" + amount + ")";
    }
}
```

Flyweight nie przechowuje opisu ani kwoty pojedynczej transakcji.
Metoda `render` dostaje je jako stan zewnętrzny.

## 2. Fabryka współdzielonych stylów

Plik: `src\main\java\com\delta\bank\lab\patterns\flyweight\TransactionStyleFactory.java`

```java
package com.delta.bank.lab.patterns.flyweight;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TransactionStyleFactory {

    private final ConcurrentMap<TransactionType, TransactionStyle> cache =
            new ConcurrentHashMap<>();

    public TransactionStyle styleFor(TransactionType type) {
        Objects.requireNonNull(type, "type");
        return cache.computeIfAbsent(type, TransactionStyleFactory::createStyle);
    }

    private static TransactionStyle createStyle(TransactionType type) {
        return switch (type) {
            case DEPOSIT -> new TransactionStyle("Deposit", "+");
            case WITHDRAWAL -> new TransactionStyle("Withdrawal", "-");
            case TRANSFER -> new TransactionStyle("Transfer", ">");
        };
    }
}
```

Fabryka przechowuje po jednym współdzielonym stylu dla każdego typu.
`computeIfAbsent` bezpiecznie oblicza wartość przy pierwszym użyciu,
również gdy kilka wątków równocześnie prosi o styl tego samego typu.
Zbiór kluczy jest ograniczony przez enum.

## 3. Dane konkretnej transakcji i renderer

Plik: `src\main\java\com\delta\bank\lab\patterns\flyweight\TransactionEntry.java`

```java
package com.delta.bank.lab.patterns.flyweight;

import java.math.BigDecimal;
import java.util.Objects;

public record TransactionEntry(
        TransactionType type,
        String description,
        BigDecimal amount) {

    public TransactionEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
    }
}
```

Plik: `src\main\java\com\delta\bank\lab\patterns\flyweight\StatementRenderer.java`

```java
package com.delta.bank.lab.patterns.flyweight;

import java.util.List;
import java.util.Objects;

public final class StatementRenderer {

    private final TransactionStyleFactory styles;

    public StatementRenderer(TransactionStyleFactory styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
    }

    public String render(TransactionEntry entry) {
        Objects.requireNonNull(entry, "entry");
        TransactionStyle style = styles.styleFor(entry.type());
        return style.render(entry.description(), entry.amount().toPlainString());
    }

    public List<String> renderAll(List<TransactionEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        return List.copyOf(entries).stream()
                .map(this::render)
                .toList();
    }
}
```

`TransactionEntry` niesie stan zewnętrzny, a `TransactionStyle` jest
współdzielonym stanem wewnętrznym. `renderAll` tworzy wynikowe napisy,
ale nie tworzy nowego stylu dla każdej pozycji.

## 4. Specyfikacja Spocka

Plik: `src\test\groovy\com\delta\bank\lab\patterns\flyweight\FlyweightSpec.groovy`

```groovy
package com.delta.bank.lab.patterns.flyweight

import spock.lang.Specification

class FlyweightSpec extends Specification {

    def "factory returns the same immutable flyweight for a type"() {
        given:
        def factory = new TransactionStyleFactory()

        when:
        def first = factory.styleFor(TransactionType.DEPOSIT)
        def second = factory.styleFor(TransactionType.DEPOSIT)

        then:
        first.is(second)
        first == new TransactionStyle("Deposit", "+")
    }

    def "different transaction types receive different styles"() {
        given:
        def factory = new TransactionStyleFactory()

        expect:
        !factory.styleFor(TransactionType.DEPOSIT)
                .is(factory.styleFor(TransactionType.WITHDRAWAL))
    }

    def "renderer shares a style but uses each entry's external state"() {
        given:
        def styles = new TransactionStyleFactory()
        def renderer = new StatementRenderer(styles)
        def first = new TransactionEntry(
                TransactionType.DEPOSIT, "salary", new BigDecimal("1000.00"))
        def second = new TransactionEntry(
                TransactionType.DEPOSIT, "refund", new BigDecimal("25.00"))

        expect:
        renderer.render(first) == "+ Deposit: salary (1000.00)"
        renderer.render(second) == "+ Deposit: refund (25.00)"
        styles.styleFor(first.type()).is(styles.styleFor(second.type()))
    }

    def "renderAll preserves order and returns immutable result"() {
        given:
        def renderer = new StatementRenderer(new TransactionStyleFactory())
        def entries = [
                new TransactionEntry(TransactionType.DEPOSIT, "salary", new BigDecimal("100.00")),
                new TransactionEntry(TransactionType.WITHDRAWAL, "rent", new BigDecimal("40.00"))
        ]

        when:
        def lines = renderer.renderAll(entries)

        then:
        lines == [
                "+ Deposit: salary (100.00)",
                "- Withdrawal: rent (40.00)"
        ]

        when:
        lines.add("another line")

        then:
        thrown(UnsupportedOperationException)
    }

    def "factory rejects a null type"() {
        when:
        new TransactionStyleFactory().styleFor(null)

        then:
        thrown(NullPointerException)
    }
}
```

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.patterns.flyweight.FlyweightSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.patterns.flyweight.FlyweightSpec"
```

## Eksperymenty

1. Dodaj wiele transakcji tego samego typu i opisz, które pola są
   współdzielone, a które pozostają specyficzne dla wpisu.
2. Dodaj nowy typ do enuma i obsłuż go w fabryce. Co sprawi, że `switch`
   bez `default` jest tu pomocny?
3. Zmień `TransactionStyle` tak, by przechowywał również kwotę.
   Dlaczego przestaje być dobrym Flyweightem?
4. Zastąp cache przez `EnumMap`. Czy nadal potrzebujesz synchronizacji?
   Jak zmieniłoby się rozwiązanie, gdyby klucze nie były enumem?
5. Zastanów się, jak zmierzyć, czy Flyweight faktycznie daje korzyść.
   Porównaj liczbę stylów i wpisów, a nie tylko czas pojedynczego testu.

## Jak odpowiedzieć na rozmowie

**Flyweight** ogranicza liczbę podobnych obiektów przez współdzielenie
ich niezmiennego stanu wewnętrznego. Stan zależny od konkretnego użycia
jest przekazywany z zewnątrz, zamiast być przechowywany w każdym
współdzielonym obiekcie.

Zaletą może być mniejsze zużycie pamięci przy bardzo dużej liczbie
obiektów. Kosztem jest bardziej złożony podział stanu i odpowiedzialność
za bezpieczne współdzielenie — Flyweight powinien być niemutowalny lub
odpowiednio synchronizowany. Wzorzec ma sens po stwierdzeniu, że powtarzane
dane są istotnym kosztem; nie warto dodawać cache bez potrzeby.

**Porównanie z Prototype:** Prototype tworzy nowe obiekty przez kopiowanie
egzemplarza, a Flyweight zmniejsza liczbę obiektów przez współdzielenie
tych samych egzemplarzy.

## Pytania kontrolne

1. Które pola w tym przykładzie są stanem wewnętrznym, a które zewnętrznym?
2. Dlaczego współdzielony Flyweight powinien być niezmienny?
3. Jakie korzyści i koszty ma cache w `TransactionStyleFactory`?
4. Czym Flyweight różni się od Prototype i Singletona?
5. Jak sprawdzić, czy wzorzec faktycznie zmniejsza zużycie pamięci?

## Do zapamiętania

- Flyweight współdzieli powtarzany, zwykle niezmienny stan wewnętrzny.
- Stan specyficzny dla pojedynczego użycia przekazuj z zewnątrz.
- Wzorzec optymalizacyjny warto stosować po rozpoznaniu realnego kosztu.
