package com.delta.bank.lab.patterns.behavioral2.command;

import java.math.BigDecimal;
import java.util.Objects;

// DepositCommand deleguje wykonanie do BankAccount (receiver).

public record DepositCommand(BankAccount account, BigDecimal amount) implements BankCommand {

    public DepositCommand {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(amount, "amount");
    }

    @Override
    public void execute() {
        account.deposit(amount);
    }
}