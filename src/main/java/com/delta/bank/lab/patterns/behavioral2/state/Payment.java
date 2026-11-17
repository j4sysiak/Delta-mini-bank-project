package com.delta.bank.lab.patterns.behavioral2.state;

// Kontekst (Payment) deleguje zachowanie do aktualnego stanu.
// Dozwolone przejścia są opisane w klasach stanów, bez rozbudowanego switch w kontekście.
// klasy stanów to: Pending, Paid, Cancelled

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