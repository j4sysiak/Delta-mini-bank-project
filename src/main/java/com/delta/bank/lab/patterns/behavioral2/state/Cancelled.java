package com.delta.bank.lab.patterns.behavioral2.state;

public final class Cancelled implements PaymentState {
    @Override public String name() {
        return "CANCELLED";
    }

    @Override public void pay(Payment payment) {
        throw new IllegalStateException("cancelled payment cannot be paid");
    }

    @Override public void cancel(Payment payment) {
        throw new IllegalStateException("already cancelled");
    }
}