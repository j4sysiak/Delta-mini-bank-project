package com.delta.bank.lab.patterns.structural.proxy;

import com.delta.bank.lab.patterns.structural.adapter.PaymentGateway;

import java.math.BigDecimal;
import java.util.Objects;

public final class TransferLimitProxy implements PaymentGateway {

    private final PaymentGateway paymentGatewayDelegate;
    private final BigDecimal limit;

    public TransferLimitProxy(PaymentGateway paymentGateway, BigDecimal limit) {
        this.paymentGatewayDelegate = Objects.requireNonNull(paymentGateway, "paymentGateway");
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
        // wywołanie metody transfer(...) na obiekcie paymentGatewayDelegate, który jest delegatem
        // dlaczego delegat?
        // bo TransferLimitProxy nie wykonuje przelewu samodzielnie, tylko deleguje to zadanie obiektowi paymentGatewayDelegate.
        paymentGatewayDelegate.transfer(from, to, amount);
    }
}