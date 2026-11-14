package com.delta.bank.lab.patterns.creational;

import java.math.BigDecimal;

public sealed interface Account {

    String id();

    record Checking(String id, BigDecimal overdraftLimit) implements Account {}

    record Savings(String id, BigDecimal interestRate) implements Account {}

    record Business(String id, String companyName) implements Account {}
}