# Lab 39 — Wzorce projektowe w Java 21 (1/4): kreacyjne

## Plan serii "Wzorce na rozmowę kwalifikacyjną"

| Lab | Temat | Wzorce |
|-----|-------|--------|
| **39** | Kreacyjne | Factory Method / Simple Factory, Builder, Singleton (enum) |
| 40 | Behawioralne I | Strategy, Observer |
| 41 | Behawioralne II | Template Method, Command, State (sealed + switch) |
| 42 | Strukturalne | Decorator, Adapter, Facade, Proxy |

Każdy lab: implementacja "po nowemu" (records, sealed, switch patterns),
test oraz sekcja "Jak odpowiedzieć na rozmowie".

## Dlaczego ten temat?

Pytania o wzorce są klasykiem rozmów. Dobra odpowiedź to: nazwa,
problem, który wzorzec rozwiązuje, krótki przykład i wady.
W Java 21 część wzorców jest dużo krótsza dzięki records, sealed
interfaces i pattern matching.

Dodaj tylko wskazane pliki. Nie zmieniaj istniejącego kodu ani Gradle.
Ćwiczenie działa bez Springa, bazy, sieci i preview.

## 1. Factory Method (sealed + switch)

Plik: `src\main\java\com\delta\bank\lab\patterns\creational\Account.java`

```java
package com.delta.bank.lab.patterns.creational;

import java.math.BigDecimal;

public sealed interface Account {

    String id();

    record Checking(String id, BigDecimal overdraftLimit) implements Account {}

    record Savings(String id, BigDecimal interestRate) implements Account {}

    record Business(String id, String companyName) implements Account {}
}
```

Plik: `...\creational\AccountFactory.java`

```java
package com.delta.bank.lab.patterns.creational;

import java.math.BigDecimal;
import java.util.Objects;

public final class AccountFactory {

    public enum Type { CHECKING, SAVINGS, BUSINESS }

    private AccountFactory() {}

    public static Account create(Type type, String id) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
        return switch (type) {
            case CHECKING -> new Account.Checking(id, new BigDecimal("500.00"));
            case SAVINGS -> new Account.Savings(id, new BigDecimal("0.03"));
            case BUSINESS -> new Account.Business(id, "N/A");
        };
    }
}
```

Switch na `enum` bez `default` jest wyczerpujący — dodanie nowego
typu bez obsługi da błąd kompilacji (zaleta nad `if/else`).

## 2. Builder (obiekt z wieloma opcjonalnymi polami)

Plik: `...\creational\TransferRequest.java`

```java
package com.delta.bank.lab.patterns.creational;

import java.math.BigDecimal;
import java.util.Objects;

public record TransferRequest(String from, String to, BigDecimal amount, String memo) {

    public TransferRequest {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("same account");
        }
        memo = memo == null ? "" : memo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String from;
        private String to;
        private BigDecimal amount;
        private String memo;

        public Builder from(String from) { this.from = from; return this; }
        public Builder to(String to) { this.to = to; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder memo(String memo) { this.memo = memo; return this; }

        public TransferRequest build() {
            return new TransferRequest(from, to, amount, memo);
        }
    }
}
```

Walidacja siedzi w kompaktowym konstruktorze recordu, więc nie da się
obejść jej bez buildera.

## 3. Singleton jako `enum`

Plik: `...\creational\IdGenerator.java`

```java
package com.delta.bank.lab.patterns.creational;

import java.util.concurrent.atomic.AtomicLong;

public enum IdGenerator {
    INSTANCE;

    private final AtomicLong counter = new AtomicLong();

    public String next(String prefix) {
        return prefix + "-" + counter.incrementAndGet();
    }
}
```

Enum daje: bezpieczeństwo wątkowe inicjalizacji, odporność na
serializację i refleksję. Wada: trudniej mockować — w prawdziwych
aplikacjach wolimy wstrzykiwanie zależności (Spring bean singleton).

## 4. Specyfikacja Spocka

Plik: `src\test\groovy\com\delta\bank\lab\patterns\creational\CreationalPatternsSpec.groovy`

```groovy
package com.delta.bank.lab.patterns.creational

import spock.lang.Specification

class CreationalPatternsSpec extends Specification {

    def "factory creates the right subtype"() {
        expect:
        AccountFactory.create(type, "A-1").class == expected

        where:
        type                             || expected
        AccountFactory.Type.CHECKING     || Account.Checking
        AccountFactory.Type.SAVINGS      || Account.Savings
        AccountFactory.Type.BUSINESS     || Account.Business
    }

    def "factory rejects null"() {
        when:
        AccountFactory.create(null, "A-1")

        then:
        thrown(NullPointerException)
    }

    def "builder creates a valid request with empty memo by default"() {
        when:
        def request = TransferRequest.builder()
                .from("PLN-1").to("PLN-2").amount(new BigDecimal("10.00")).build()

        then:
        request.memo() == ""
        request.amount() == new BigDecimal("10.00")
    }

    def "builder validates through the record constructor"() {
        when:
        TransferRequest.builder().from("PLN-1").to(to).amount(amount).build()

        then:
        thrown(expected)

        where:
        to       | amount                  || expected
        "PLN-1"  | new BigDecimal("1.00")  || IllegalArgumentException
        "PLN-2"  | BigDecimal.ZERO         || IllegalArgumentException
        "PLN-2"  | null                    || NullPointerException
    }

    def "enum singleton is one instance and generates unique ids"() {
        expect:
        IdGenerator.INSTANCE.is(IdGenerator.valueOf("INSTANCE"))

        when:
        def first = IdGenerator.INSTANCE.next("TX")
        def second = IdGenerator.INSTANCE.next("TX")

        then:
        first != second
        first.startsWith("TX-")
    }
}
```

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.patterns.creational.CreationalPatternsSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.patterns.creational.CreationalPatternsSpec"
```

## Eksperymenty

1. Dodaj `Type.STUDENT` do enuma i zobacz błąd kompilacji w `switch`.
2. Zamień `AccountFactory` na wersję z `Map<Type, Function<String, Account>>` — kiedy to lepsze?
3. Dodaj do buildera metodę `fromAccount(Account)` i zrób test.
4. Napisz klasyczny singleton z `synchronized` i double-checked locking (`volatile`) — porównaj z enumem.
5. Zbuduj `TransferRequest` bez buildera, konstruktorem z 4 `String`/`BigDecimal` — co jest mniej czytelne?

## Jak odpowiedzieć na rozmowie

- **Factory:** ukrywa tworzenie i wybór typu; wywołujący zależy od interfejsu. Wada: rośnie `switch` (rozwiązanie: rejestr/mapa lub Spring).
- **Builder:** czytelne tworzenie obiektów z wieloma parametrami; unika "teleskopowych" konstruktorów. W Javie 21 dla prostych danych często wystarcza record.
- **Singleton:** jedna instancja globalnie. Najlepsza implementacja: `enum` (Joshua Bloch). Wada: ukryta zależność globalna, trudne testy.

## Pytania kontrolne

1. Czym różni się Factory Method od Abstract Factory?
2. Dlaczego double-checked locking wymaga `volatile`?
3. Dlaczego `enum` jest odporny na serializację i refleksję?
4. Kiedy builder jest zbędny dzięki recordom (i named arguments w innych językach)?

## Do zapamiętania

- Sealed + records + `switch` = factory z kontrolą kompilatora.
- Walidacja inwariantów w kompaktowym konstruktorze recordu.
- Singleton = `enum` lub bean w kontenerze DI, nie ręczne `static getInstance()`.
