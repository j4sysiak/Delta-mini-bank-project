package com.delta.bank.lab.patterns.behavioral2.state;

public final class Pending implements PaymentState {
    @Override public String name() { return "PENDING"; }
    @Override public void pay(Payment payment) { payment.changeState(new Paid()); }
    @Override public void cancel(Payment payment) { payment.changeState(new Cancelled()); }
}