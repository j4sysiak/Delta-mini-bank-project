package com.delta.bank.lab.patterns.behavioral2.state;

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