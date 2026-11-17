package com.delta.bank.lab.patterns.behavioral2.state;

public final class Paid implements PaymentState {
    @Override public String name() {
        return "PAID";
    }

    @Override public void pay(Payment payment) {
        throw new IllegalStateException("already paid");
    }

    @Override public void cancel(Payment payment) {
        throw new IllegalStateException("paid payment cannot be cancelled");
    }
}