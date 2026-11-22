package com.delta.bank.lab.patterns.creational.abstractfactory;

import java.math.BigDecimal;
import java.util.Objects;

public record TransferNotice(String recipient, BigDecimal amount) {

    public TransferNotice {
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(amount, "amount");
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be blank");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}