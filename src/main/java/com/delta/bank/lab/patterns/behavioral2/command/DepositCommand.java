package com.delta.bank.lab.patterns.behavioral2.command;

import java.math.BigDecimal;
import java.util.Objects;

public record DepositCommand(BankAccount account, BigDecimal amount) implements BankCommand {

    // `DepositCommand` to polecenie wpłaty do BankAccount (odbiorcy): przechowuje konto i kwotę,
    // a po wywołaniu execute() zleca account.deposit(amount).
    // `BankAccount` pełni rolę odbiorcy (receiver) w wzorcu Command.
    public DepositCommand {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(amount, "amount");
    }

    @Override
    public void execute() {
        account.deposit(amount);
    }
}