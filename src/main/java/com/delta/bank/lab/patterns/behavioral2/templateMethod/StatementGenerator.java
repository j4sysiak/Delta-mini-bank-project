package com.delta.bank.lab.patterns.behavioral2.templateMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public abstract class StatementGenerator {

    public record Transaction(String description, BigDecimal amount) {
        public Transaction {
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(amount, "amount");
        }
    }

    public final String generate(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions");
        StringBuilder result = new StringBuilder(header());
        for (Transaction transaction : List.copyOf(transactions)) {
            result.append(format(transaction)).append('\n');
        }
        return result.append(footer()).toString();
    }

    protected abstract String header();

    protected abstract String format(Transaction transaction);

    protected String footer() {
        return "END\n";
    }
}