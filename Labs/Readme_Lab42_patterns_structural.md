# Lab 42 — Wzorce projektowe w Java 21 (4/4): Decorator, Adapter, Facade i Proxy

Seria: 39 kreacyjne → 40 Strategy, Observer → 41 Template Method, Command, State → **42 wzorce strukturalne**.

## Dlaczego ten temat?

Wzorce strukturalne pomagają łączyć obiekty i rozszerzać ich zachowanie
bez niepotrzebnego dziedziczenia. W tym labie zbudujesz mały przepływ
przelewu:

- **Decorator** dodaje minimalną opłatę do kalkulatora,
- **Adapter** tłumaczy współczesne API na starszy gateway przyjmujący grosze,
- **Facade** udostępnia prostą operację przelewu i koordynuje komponenty,
- **Proxy** ogranicza maksymalną kwotę przed wywołaniem gatewaya.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj istniejącego kodu
ani konfiguracji Gradle. Ćwiczenie działa bez Springa, bazy i sieci.

## 1. Decorator — minimalna opłata

Plik: `src\main\java\com\delta\bank\lab\patterns\structural\FeeCalculator.java`

```java
package com.delta.bank.lab.patterns.structural;

import java.math.BigDecimal;

@FunctionalInterface
public interface FeeCalculator {
    BigDecimal calculate(BigDecimal amount);
}
```

Plik: `...\\structural\\PercentageFeeCalculator.java`

```java
package com.delta.bank.lab.patterns.structural;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class PercentageFeeCalculator implements FeeCalculator {

    private final BigDecimal rate;

    public PercentageFeeCalculator(BigDecimal rate) {
        this.rate = Objects.requireNonNull(rate, "rate");
        if (rate.signum() < 0) {
            throw new IllegalArgumentException("rate must not be negative");
        }
    }

    @Override
    public BigDecimal calculate(BigDecimal amount) {
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
```

Plik: `...\\structural\\MinimumFeeDecorator.java`

```java
package com.delta.bank.lab.patterns.structural;

import java.math.BigDecimal;
import java.util.Objects;

public final class MinimumFeeDecorator implements FeeCalculator {

    private final FeeCalculator delegate;
    private final BigDecimal minimum;

    public MinimumFeeDecorator(FeeCalculator delegate, BigDecimal minimum) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.minimum = Objects.requireNonNull(minimum, "minimum");
        if (minimum.signum() < 0) {
            throw new IllegalArgumentException("minimum must not be negative");
        }
    }

    @Override
    public BigDecimal calculate(BigDecimal amount) {
        BigDecimal calculated = delegate.calculate(amount);
        return calculated.compareTo(minimum) < 0 ? minimum : calculated;
    }
}
```

Decorator ma ten sam interfejs co obiekt dekorowany, więc można go
opakowywać kolejnymi dekoratorami bez zmiany kodu kalkulatora bazowego.

## 2. Adapter — stare API przyjmujące grosze

Plik: `...\\structural\\PaymentGateway.java`

```java
package com.delta.bank.lab.patterns.structural;

import java.math.BigDecimal;

public interface PaymentGateway {
    void transfer(String from, String to, BigDecimal amount);
}
```

Plik: `...\\structural\\LegacyPaymentGateway.java`

```java
package com.delta.bank.lab.patterns.structural;

public interface LegacyPaymentGateway {
    void send(String from, String to, long amountInCents);
}
```

Plik: `...\\structural\\LegacyPaymentGatewayAdapter.java`

```java
package com.delta.bank.lab.patterns.structural;

import java.math.BigDecimal;
import java.util.Objects;

public final class LegacyPaymentGatewayAdapter implements PaymentGateway {

    private final LegacyPaymentGateway legacy;

    public LegacyPaymentGatewayAdapter(LegacyPaymentGateway legacy) {
        this.legacy = Objects.requireNonNull(legacy, "legacy");
    }

    @Override
    public void transfer(String from, String to, BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        long cents = amount.movePointRight(2).longValueExact();
        legacy.send(from, to, cents);
    }
}
```

`longValueExact()` nie zaokrągla po cichu. Kwota z ułamkiem grosza
albo poza zakresem `long` kończy się wyjątkiem.

## 3. Proxy — limit kwoty

Plik: `...\\structural\\TransferLimitProxy.java`

```java
package com.delta.bank.lab.patterns.structural;

import java.math.BigDecimal;
import java.util.Objects;

public final class TransferLimitProxy implements PaymentGateway {

    private final PaymentGateway delegate;
    private final BigDecimal limit;

    public TransferLimitProxy(PaymentGateway delegate, BigDecimal limit) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.limit = Objects.requireNonNull(limit, "limit");
        if (limit.signum() < 0) {
            throw new IllegalArgumentException("limit must not be negative");
        }
    }

    @Override
    public void transfer(String from, String to, BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.compareTo(limit) > 0) {
            throw new IllegalArgumentException("transfer exceeds limit");
        }
        delegate.transfer(from, to, amount);
    }
}
```

Proxy zachowuje interfejs gatewaya i kontroluje dostęp do obiektu
docelowego. W prawdziwym systemie limity i autoryzacja muszą być
egzekwowane po stronie serwera — sam proxy w aplikacji nie jest granicą
bezpieczeństwa.

## 4. Facade — proste API przelewu

Plik: `...\\structural\\TransferReceipt.java`

```java
package com.delta.bank.lab.patterns.structural;

import java.math.BigDecimal;

public record TransferReceipt(
        String from,
        String to,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal totalDebit) {}
```

Plik: `...\\structural\\TransferFacade.java`

```java
package com.delta.bank.lab.patterns.structural;

import java.math.BigDecimal;
import java.util.Objects;

public final class TransferFacade {

    private final PaymentGateway gateway;
    private final FeeCalculator feeCalculator;

    public TransferFacade(PaymentGateway gateway, FeeCalculator feeCalculator) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.feeCalculator = Objects.requireNonNull(feeCalculator, "feeCalculator");
    }

    public TransferReceipt transfer(String from, String to, BigDecimal amount) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("accounts must be different");
        }

        BigDecimal fee = feeCalculator.calculate(amount);
        gateway.transfer(from, to, amount);
        return new TransferReceipt(from, to, amount, fee, amount.add(fee));
    }
}
```

Facade upraszcza użycie kilku obiektów. Ułóż komponenty przez kompozycję,
na przykład: `TransferFacade(new TransferLimitProxy(adapter, limit), feeCalculator)`.

## 5. Specyfikacja Spocka

Plik: `src\test\groovy\com\delta\bank\lab\patterns\structural\StructuralPatternsSpec.groovy`

```groovy
package com.delta.bank.lab.patterns.structural

import spock.lang.Specification

class StructuralPatternsSpec extends Specification {

    def "minimum fee decorator raises a smaller fee"() {
        given:
        FeeCalculator percentage = new PercentageFeeCalculator(new BigDecimal("0.01"))
        FeeCalculator calculator = new MinimumFeeDecorator(percentage, new BigDecimal("2.00"))

        expect:
        calculator.calculate(new BigDecimal("10.00")) == new BigDecimal("2.00")
    }

    def "minimum fee decorator preserves a larger calculated fee"() {
        given:
        FeeCalculator percentage = new PercentageFeeCalculator(new BigDecimal("0.01"))
        FeeCalculator calculator = new MinimumFeeDecorator(percentage, new BigDecimal("2.00"))

        expect:
        calculator.calculate(new BigDecimal("500.00")) == new BigDecimal("5.00")
    }

    def "adapter converts zlotys to exact grosze for the legacy API"() {
        given:
        def legacy = Mock(LegacyPaymentGateway)
        def adapter = new LegacyPaymentGatewayAdapter(legacy)

        when:
        adapter.transfer("PLN-1", "PLN-2", new BigDecimal("12.34"))

        then:
        1 * legacy.send("PLN-1", "PLN-2", 1234L)
    }

    def "adapter rejects fractions of a cent instead of rounding"() {
        given:
        def legacy = Mock(LegacyPaymentGateway)
        def adapter = new LegacyPaymentGatewayAdapter(legacy)

        when:
        adapter.transfer("PLN-1", "PLN-2", new BigDecimal("1.001"))

        then:
        thrown(ArithmeticException)
        0 * legacy.send(_, _, _)
    }

    def "proxy forwards an allowed transfer"() {
        given:
        def delegate = Mock(PaymentGateway)
        def proxy = new TransferLimitProxy(delegate, new BigDecimal("100.00"))

        when:
        proxy.transfer("PLN-1", "PLN-2", new BigDecimal("50.00"))

        then:
        1 * delegate.transfer("PLN-1", "PLN-2", new BigDecimal("50.00"))
    }

    def "proxy blocks an over-limit transfer before delegating"() {
        given:
        def delegate = Mock(PaymentGateway)
        def proxy = new TransferLimitProxy(delegate, new BigDecimal("100.00"))

        when:
        proxy.transfer("PLN-1", "PLN-2", new BigDecimal("100.01"))

        then:
        thrown(IllegalArgumentException)
        0 * delegate.transfer(_, _, _)
    }

    def "facade coordinates fee calculation and gateway call"() {
        given:
        def gateway = Mock(PaymentGateway)
        FeeCalculator fees = new MinimumFeeDecorator(
                new PercentageFeeCalculator(new BigDecimal("0.01")),
                new BigDecimal("2.00"))
        def facade = new TransferFacade(gateway, fees)

        when:
        def receipt = facade.transfer("PLN-1", "PLN-2", new BigDecimal("10.00"))

        then:
        1 * gateway.transfer("PLN-1", "PLN-2", new BigDecimal("10.00"))
        receipt.fee() == new BigDecimal("2.00")
        receipt.totalDebit() == new BigDecimal("12.00")
    }

    def "facade rejects invalid transfer without calling gateway"() {
        given:
        def gateway = Mock(PaymentGateway)
        def facade = new TransferFacade(gateway, amount -> BigDecimal.ZERO)

        when:
        facade.transfer("PLN-1", "PLN-1", BigDecimal.TEN)

        then:
        thrown(IllegalArgumentException)
        0 * gateway.transfer(_, _, _)
    }
}
```

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.patterns.structural.StructuralPatternsSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.patterns.structural.StructuralPatternsSpec"
```

## Eksperymenty

1. Dodaj dekorator zaokrąglający opłatę do pełnych złotych. Połącz go z `MinimumFeeDecorator` w obu możliwych kolejnościach. Czy kolejność ma znaczenie?
2. Zaimplementuj adapter w drugą stronę: stary gateway ma przyjmować `long`, a nowy interfejs kwotę w `BigDecimal`. Jak uniknąć utraty skali?
3. Rozszerz proxy o limit dzienny. Jakich dodatkowych danych i synchronizacji potrzebujesz?
4. Dodaj do Facade drugą operację, która najpierw sprawdza limit, a potem wykonuje przelew. Czy Facade powinien znać szczegóły wszystkich komponentów?
5. Zastanów się, czy `TransferLimitProxy` to proxy, czy raczej decorator. Uzasadnij odpowiedź jego intencją, nie tylko kształtem kodu.

## Jak odpowiedzieć na rozmowie

- **Decorator:** dokłada odpowiedzialność, zachowując interfejs; można składać zachowania. Wada: wiele małych obiektów i trudniejsze debugowanie.
- **Adapter:** tłumaczy niezgodne interfejsy, aby współpracowały. Nie zmienia logiki klienta ani starej biblioteki.
- **Facade:** daje prostszy punkt wejścia do podsystemu. Nie musi ukrywać wszystkich możliwości ani być jedyną drogą do komponentów.
- **Proxy:** kontroluje dostęp do obiektu o tym samym interfejsie, np. przez autoryzację, cache, lazy loading lub zdalne wywołanie.
- **Decorator a Proxy:** struktura może wyglądać podobnie; rozróżnia je cel — rozszerzanie zachowania kontra kontrola dostępu.

## Pytania kontrolne

1. Dlaczego Adapter i Proxy implementują interfejs oczekiwany przez klienta?
2. Kiedy zastosować Facade, a kiedy Adapter?
3. Czy można zagnieżdżać dekoratory? Jak wpływa na wynik ich kolejność?
4. Dlaczego limit w klienckim Proxy nie wystarczy do zabezpieczenia operacji finansowej?

## Do zapamiętania

- Decorator dodaje zachowanie, Adapter tłumaczy interfejs, Facade upraszcza użycie, Proxy kontroluje dostęp.
- Podobny kształt klas nie oznacza tego samego wzorca — liczy się intencja.
- Składaj małe obiekty przez kompozycję i testuj delegację oraz przypadki, w których delegacja nie powinna nastąpić.
