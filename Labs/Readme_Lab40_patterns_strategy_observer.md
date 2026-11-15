# Lab 40 — Wzorce projektowe w Java 21 (2/4): Strategy i Observer

Seria: 39 kreacyjne → **40 Strategy, Observer** → 41 Template/Command/State → 42 strukturalne.

## Dlaczego ten temat?

Strategy i Observer to dwa najczęściej pytane wzorce behawioralne.
W Javie 21 Strategy często sprowadza się do interfejsu funkcyjnego
i lambdy, a Observer do listy listenerów (albo sealed events).

Dodaj tylko wskazane pliki. Nie zmieniaj istniejącego kodu ani Gradle.
Ćwiczenie działa bez Springa, bazy, sieci i preview.

## 1. Strategy — opłaty za przelew

Plik: `src\main\java\com\delta\bank\lab\patterns\behavioral\FeeStrategy.java`

```java
package com.delta.bank.lab.patterns.behavioral;

import java.math.BigDecimal;
import java.math.RoundingMode;

@FunctionalInterface
public interface FeeStrategy {

    BigDecimal feeFor(BigDecimal amount);

    FeeStrategy FLAT = amount -> new BigDecimal("2.00");

    FeeStrategy PERCENT = amount ->
            amount.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);

    FeeStrategy FREE = amount -> BigDecimal.ZERO.setScale(2);
}
```

Plik: `...\behavioral\TransferService.java`

```java
package com.delta.bank.lab.patterns.behavioral;

import java.math.BigDecimal;
import java.util.Objects;

public class TransferService {

    private final FeeStrategy feeStrategy;

    public TransferService(FeeStrategy feeStrategy) {
        this.feeStrategy = Objects.requireNonNull(feeStrategy);
    }

    public BigDecimal totalCost(BigDecimal amount) {
        return amount.add(feeStrategy.feeFor(amount));
    }
}
```

Strategię wybiera się przy tworzeniu (lub w locie) — `TransferService`
nie zawiera `if (type == ...)`.

## 2. Observer — zdarzenia o saldzie

Plik: `...\behavioral\BalanceEvent.java`

```java
package com.delta.bank.lab.patterns.behavioral;

import java.math.BigDecimal;

public sealed interface BalanceEvent {

    String accountId();

    record Deposited(String accountId, BigDecimal amount) implements BalanceEvent {}

    record Withdrawn(String accountId, BigDecimal amount) implements BalanceEvent {}
}
```

Plik: `...\behavioral\ObservableAccount.java`

```java
package com.delta.bank.lab.patterns.behavioral;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ObservableAccount {

    private final String id;
    private BigDecimal balance = BigDecimal.ZERO;
    private final List<Consumer<BalanceEvent>> listeners = new CopyOnWriteArrayList<>();

    public ObservableAccount(String id) {
        this.id = id;
    }

    public Runnable subscribe(Consumer<BalanceEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void deposit(BigDecimal amount) {
        balance = balance.add(amount);
        publish(new BalanceEvent.Deposited(id, amount));
    }

    public void withdraw(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient funds");
        }
        balance = balance.subtract(amount);
        publish(new BalanceEvent.Withdrawn(id, amount));
    }

    public BigDecimal balance() {
        return balance;
    }

    private void publish(BalanceEvent event) {
        listeners.forEach(l -> l.accept(event));
    }
}
```

`CopyOnWriteArrayList` pozwala bezpiecznie wypisać się (`unsubscribe`)
w trakcie powiadamiania. `subscribe` zwraca `Runnable` do wypisania.

## 3. Specyfikacja Spocka

Plik: `src\test\groovy\com\delta\bank\lab\patterns\behavioral\BehavioralPatternsSpec.groovy`

```groovy
package com.delta.bank.lab.patterns.behavioral

import spock.lang.Specification

class BehavioralPatternsSpec extends Specification {

    def "strategy changes the fee without changing the service"() {
        expect:
        new TransferService(strategy).totalCost(new BigDecimal("100.00")) == total

        where:
        strategy              || total
        FeeStrategy.FLAT      || new BigDecimal("102.00")
        FeeStrategy.PERCENT   || new BigDecimal("101.00")
        FeeStrategy.FREE      || new BigDecimal("100.00")
    }

    def "strategy can be a lambda"() {
        given:
        FeeStrategy custom = { amount -> new BigDecimal("5.00") }

        expect:
        new TransferService(custom).totalCost(new BigDecimal("10.00")) == new BigDecimal("15.00")
    }

    def "service rejects null strategy"() {
        when:
        new TransferService(null)

        then:
        thrown(NullPointerException)
    }

    def "observers receive events in order"() {
        given:
        def account = new ObservableAccount("PLN-1")
        def events = []
        account.subscribe { events << it }

        when:
        account.deposit(new BigDecimal("50.00"))
        account.withdraw(new BigDecimal("20.00"))

        then:
        events*.getClass() == [BalanceEvent.Deposited, BalanceEvent.Withdrawn]
        events[0].amount() == new BigDecimal("50.00")
        account.balance() == new BigDecimal("30.00")
    }

    def "unsubscribe stops notifications"() {
        given:
        def account = new ObservableAccount("PLN-1")
        def count = 0
        def unsubscribe = account.subscribe { count++ }

        when:
        account.deposit(new BigDecimal("1.00"))
        unsubscribe.run()
        account.deposit(new BigDecimal("1.00"))

        then:
        count == 1
    }

    def "failed withdraw publishes nothing"() {
        given:
        def account = new ObservableAccount("PLN-1")
        def listener = Mock(java.util.function.Consumer)
        account.subscribe(listener)

        when:
        account.withdraw(new BigDecimal("1.00"))

        then:
        thrown(IllegalStateException)
        0 * listener.accept(_)
    }

    def "pattern matching switch handles events"() {
        given:
        BalanceEvent event = new BalanceEvent.Deposited("PLN-1", BigDecimal.TEN)

        expect:
        event instanceof BalanceEvent.Deposited
    }
}
```

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.patterns.behavioral.strategy.StrategyPatternsSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.patterns.behavioral.strategy.StrategyPatternsSpec"
```

## Eksperymenty

1. Dodaj w Javie metodę `describe(BalanceEvent e)` ze `switch` po sealed (bez `default`) i przetestuj ją.
2. Dodaj strategię `TIERED` (progi kwot) jako `enum implements FeeStrategy`.
3. Niech listener rzuci wyjątek — co dzieje się z pozostałymi? Jak to naprawić (try/catch w `publish`)?
4. Zamień `Consumer` na własny interfejs `BalanceListener` — kiedy to lepsze?
5. Porównaj z `java.util.concurrent.Flow` (Publisher/Subscriber) — czym różni się od prostego Observera?

## Jak odpowiedzieć na rozmowie

- **Strategy:** rodzina wymiennych algorytmów za wspólnym interfejsem; zastępuje `if/switch` po typie; w Javie zwykle lambda lub enum z zachowaniem. Zasada Open/Closed.
- **Observer:** obiekt (subject) powiadamia zarejestrowanych słuchaczy o zmianie; luźne powiązanie. Uwaga na: wycieki pamięci (brak unsubscribe), kolejność, wyjątki w listenerach, wielowątkowość.
- **Różnica:** Strategy — *jak* coś zrobić (wstrzyknięty algorytm); Observer — *kto* ma się dowiedzieć o zdarzeniu.

## Pytania kontrolne

1. Dlaczego `CopyOnWriteArrayList` w listach listenerów?
2. Czym Strategy różni się od State?
3. Gdzie w JDK/Springu widzisz Strategy (`Comparator`) i Observer (`ApplicationEventPublisher`)?
4. Jak przetestować, że listener *nie* został wywołany (Spock `0 * listener.accept(_)`)?

## Do zapamiętania

- Strategy w Javie = interfejs funkcyjny + lambdy; `Comparator` to klasyczny przykład.
- Observer: zawsze zapewnij sposób wypisania się i obsługę błędów listenerów.
- Sealed events + switch = bezpieczna obsługa wszystkich typów zdarzeń.
