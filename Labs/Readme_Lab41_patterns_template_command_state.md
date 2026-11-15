# Lab 41 — Wzorce projektowe w Java 21 (3/4): Template Method, Command i State

Seria: 39 kreacyjne → 40 Strategy, Observer → **41 Template Method, Command, State** → 42 strukturalne.

## Dlaczego ten temat?

To trzy często spotykane wzorce behawioralne, które rozwiązują różne
problemy: szkielet algorytmu, enkapsulowanie żądania i zachowanie zależne
od stanu obiektu. W Labie porównasz ich intencje oraz przećwiczysz
implementację i testowanie każdego z nich.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj istniejącego kodu
ani konfiguracji Gradle. Ćwiczenie działa bez Springa, bazy i sieci.
Kompilator projektu ma włączone preview Java 21, potrzebne w eksperymencie
ze wzorcami w `switch`; podstawowe implementacje poniżej nie wymagają preview.

## 1. Template Method — szkielet generowania wyciągu

Plik: `src\main\java\com\delta\bank\lab\patterns\behavioral2\StatementGenerator.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public abstract class StatementGenerator {

    public record Transaction(String description, BigDecimal amount) {
        public Transaction {
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(amount, "amount");
        }
    }

    public final String generate(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions");
        StringBuilder result = new StringBuilder(header());
        for (Transaction transaction : List.copyOf(transactions)) {
            result.append(format(transaction)).append('\n');
        }
        return result.append(footer()).toString();
    }

    protected abstract String header();

    protected abstract String format(Transaction transaction);

    protected String footer() {
        return "END\n";
    }
}
```

Plik: `...\behavioral2\CsvStatementGenerator.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

public final class CsvStatementGenerator extends StatementGenerator {

    @Override
    protected String header() {
        return "description,amount\n";
    }

    @Override
    protected String format(Transaction transaction) {
        return transaction.description() + "," + transaction.amount().toPlainString();
    }
}
```

`generate` jest metodą szablonową: ustala kolejność kroków, a podklasy
uzupełniają wybrane kroki. Jest `final`, żeby podklasa nie mogła zmienić
algorytmu. `footer` ma implementację domyślną i można go nadpisać.

## 2. Command — kolejka poleceń

Plik: `...\behavioral2\BankAccount.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

import java.math.BigDecimal;
import java.util.Objects;

public final class BankAccount {

    private BigDecimal balance = BigDecimal.ZERO;

    public void deposit(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        balance = balance.add(amount);
    }

    public BigDecimal balance() {
        return balance;
    }
}
```

Plik: `...\behavioral2\BankCommand.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

@FunctionalInterface
public interface BankCommand {
    void execute();
}
```

Plik: `...\behavioral2\DepositCommand.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

import java.math.BigDecimal;
import java.util.Objects;

public record DepositCommand(BankAccount account, BigDecimal amount) implements BankCommand {

    public DepositCommand {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(amount, "amount");
    }

    @Override
    public void execute() {
        account.deposit(amount);
    }
}
```

Plik: `...\behavioral2\CommandQueue.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public final class CommandQueue {

    private final Queue<BankCommand> commands = new ArrayDeque<>();

    public void submit(BankCommand command) {
        commands.add(Objects.requireNonNull(command, "command"));
    }

    public boolean runNext() {
        BankCommand command = commands.poll();
        if (command == null) {
            return false;
        }
        command.execute();
        return true;
    }

    public void runAll() {
        while (runNext()) {
            // Execute queued commands in submission order.
        }
    }

    public int size() {
        return commands.size();
    }
}
```

Command opakowuje żądanie jako obiekt; kolejka (invoker) może je odroczyć.
`DepositCommand` deleguje wykonanie do `BankAccount` (receiver).

## 3. State — cykl życia płatności

Plik: `...\behavioral2\PaymentState.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

public sealed interface PaymentState permits Pending, Paid, Cancelled {

    String name();

    void pay(Payment payment);

    void cancel(Payment payment);
}
```

Plik: `...\behavioral2\Pending.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

public final class Pending implements PaymentState {
    @Override public String name() { return "PENDING"; }
    @Override public void pay(Payment payment) { payment.changeState(new Paid()); }
    @Override public void cancel(Payment payment) { payment.changeState(new Cancelled()); }
}
```

Plik: `...\behavioral2\Paid.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

public final class Paid implements PaymentState {
    @Override public String name() { return "PAID"; }
    @Override public void pay(Payment payment) { throw new IllegalStateException("already paid"); }
    @Override public void cancel(Payment payment) { throw new IllegalStateException("paid payment cannot be cancelled"); }
}
```

Plik: `...\behavioral2\Cancelled.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

public final class Cancelled implements PaymentState {
    @Override public String name() { return "CANCELLED"; }
    @Override public void pay(Payment payment) { throw new IllegalStateException("cancelled payment cannot be paid"); }
    @Override public void cancel(Payment payment) { throw new IllegalStateException("already cancelled"); }
}
```

Plik: `...\behavioral2\Payment.java`

```java
package com.delta.bank.lab.patterns.behavioral2;

public final class Payment {

    private PaymentState state = new Pending();

    public void pay() {
        state.pay(this);
    }

    public void cancel() {
        state.cancel(this);
    }

    public String status() {
        return state.name();
    }

    void changeState(PaymentState state) {
        this.state = state;
    }
}
```

Kontekst (`Payment`) deleguje zachowanie do aktualnego stanu. Dozwolone
przejścia są opisane w klasach stanów, bez rozbudowanego `switch` w kontekście.

## 4. Specyfikacja Spocka

Plik: `src\test\groovy\com\delta\bank\lab\patterns\behavioral2\BehavioralPatternsSpec.groovy`

```groovy
package com.delta.bank.lab.patterns.behavioral2

import spock.lang.Specification

class BehavioralPatternsSpec extends Specification {

    def "template method renders header rows and footer in order"() {
        given:
        def generator = new CsvStatementGenerator()
        def transactions = [
                new StatementGenerator.Transaction("salary", new BigDecimal("100.00")),
                new StatementGenerator.Transaction("rent", new BigDecimal("-25.00"))
        ]

        expect:
        generator.generate(transactions) == "description,amount\nsalary,100.00\nrent,-25.00\nEND\n"
    }

    def "template method renders an empty statement"() {
        given:
        def generator = new CsvStatementGenerator()

        expect:
        generator.generate([]) == "description,amount\nEND\n"
    }

    def "command is deferred until the invoker runs it"() {
        given:
        def account = new BankAccount()
        def queue = new CommandQueue()
        queue.submit(new DepositCommand(account, new BigDecimal("25.00")))

        expect:
        account.balance() == BigDecimal.ZERO
        queue.size() == 1

        when:
        queue.runNext()

        then:
        account.balance() == new BigDecimal("25.00")
        queue.size() == 0
    }

    def "queue runs commands in submission order"() {
        given:
        def account = new BankAccount()
        def queue = new CommandQueue()
        queue.submit(new DepositCommand(account, new BigDecimal("10.00")))
        queue.submit(new DepositCommand(account, new BigDecimal("5.00")))

        when:
        queue.runAll()

        then:
        account.balance() == new BigDecimal("15.00")
        queue.runNext() == false
    }

    def "payment transitions from pending to paid"() {
        given:
        def payment = new Payment()

        when:
        payment.pay()

        then:
        payment.status() == "PAID"
    }

    def "pending payment can be cancelled"() {
        given:
        def payment = new Payment()

        when:
        payment.cancel()

        then:
        payment.status() == "CANCELLED"
    }

    def "paid payment cannot be paid again or cancelled"() {
        given:
        def payment = new Payment()
        payment.pay()

        when:
        payment.cancel()

        then:
        thrown(IllegalStateException)
        payment.status() == "PAID"
    }
}
```

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.patterns.behavioral2.BehavioralPatternsSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.patterns.behavioral2.BehavioralPatternsSpec"
```

## Eksperymenty

1. Dodaj drugi generator (np. tekstowy) i zmień tylko kroki szablonu. Co powinno pozostać `final`?
2. Dodaj `WithdrawCommand` oraz walidację niewystarczającego salda. Jak zachowa się `runAll`, jeśli polecenie rzuci wyjątek?
3. Dodaj do poleceń `undo()` i historię wykonanych komend. Czy każde polecenie da się odwrócić?
4. Dodaj stan `Refunded` oraz dozwolone przejścia. Jak pilnuje ich `sealed interface`?
5. Zaimplementuj ten sam cykl płatności przez `switch` nad enumem statusu. Porównaj z klasycznym State. (Java 21 pattern matching w `switch` wymaga w tym projekcie preview, które jest włączone.)

## Jak odpowiedzieć na rozmowie

- **Template Method:** definiuje niezmienny szkielet algorytmu, a podklasy dostarczają wybrane kroki. Wada: dziedziczenie i silniejsze sprzężenie.
- **Command:** zamienia żądanie w obiekt; oddziela nadawcę od odbiorcy i umożliwia kolejkę, logowanie, retry lub undo (jeśli komenda potrafi się odwrócić).
- **State:** zmienia zachowanie kontekstu wraz ze stanem i przenosi reguły przejść do obiektów stanów. Nie każdy enum ze `switch` jest pełnym wzorcem State.
- **Różnica:** Template Method zmienia kroki przez dziedziczenie; Strategy (Lab 40) wymienia algorytm przez kompozycję; State enkapsuluje zachowanie zależne od cyklu życia.

## Pytania kontrolne

1. Który wzorzec opiera się na dziedziczeniu, a który na kompozycji?
2. Jakie są role Command: client, command, invoker i receiver?
3. Jak rozstrzygnąć, czy logika pasuje do State, czy wystarczy enum i `switch`?
4. Jak testować nielegalne przejścia oraz kolejkę, nie wiążąc testu ze szczegółami implementacji?

## Do zapamiętania

- Template Method: szkielet `final`, kroki jako metody abstrakcyjne/hooki.
- Command: żądanie jako obiekt; undo nie jest automatyczne.
- State: kontekst deleguje do aktualnego stanu, który określa zachowanie i przejścia.
- Wybieraj wzorzec, gdy rozwiązuje realny problem — nie dla samej nazwy.
