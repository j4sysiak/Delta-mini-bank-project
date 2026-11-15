package com.delta.bank.lab.patterns.behavioral2.state;

public sealed interface PaymentState permits Pending, Paid, Cancelled {

    String name();

    void pay(Payment payment);

    void cancel(Payment payment);
}