package com.delta.bank.lab.patterns.creational;

import java.math.BigDecimal;
import java.util.Objects;

public final class AccountFactory {

    public enum Type { CHECKING, SAVINGS, BUSINESS }

    private AccountFactory() {}

    public static Account create(Type type, String id) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
        return switch (type) {
            case CHECKING -> new Account.Checking(id, new BigDecimal("500.00"));
            case SAVINGS -> new Account.Savings(id, new BigDecimal("0.03"));
            case BUSINESS -> new Account.Business(id, "N/A");
        };
    }
}