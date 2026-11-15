package com.delta.bank.lab.patterns.behavioral2.command;

@FunctionalInterface
public interface BankCommand {
    void execute();
}