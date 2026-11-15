package com.delta.bank.lab.patterns.behavioral2.command;

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