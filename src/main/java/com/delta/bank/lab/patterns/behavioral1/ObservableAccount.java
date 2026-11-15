package com.delta.bank.lab.patterns.behavioral1;

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

    public Runnable subscribeEvent(Consumer<BalanceEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);

        /*
        @FunctionalInterface
        public interface Runnable {
          void run();
        }

        Runnable unsubscribe = new Runnable() {
            @Override
            public void run() {
                // implementacja metody run()
            }
        };

        Runnable unsubscribe = () -> {
            // implementacja metody run()
            listeners.remove(listener);
        };

        Runnable unsubscribe = () -> listeners.remove(listener);


         */
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

    // Ta metoda rozsyła zdarzenie o zmianie salda do wszystkich zapisanych obserwatorów:
    // `listeners.forEach(l -> l.accept(event))` iteruje po wszystkich obserwatorach i wywołuje ich metodę `accept`, przekazując im zdarzenie.
    private void publish(BalanceEvent event) {
        listeners.forEach(obserwator-> obserwator.accept(event));
    }
}