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

        // Kopiujemy listę przez List.copyOf(transactions), żeby generator iterował po stabilnym, niemodyfikowalnym obrazie danych.
        // Dzięki temu zmiany w oryginalnej liście w trakcie generowania nie zmienią tego, po czym iterujemy, ani nie wywołają typowego ConcurrentModificationException.
        //To płytka kopia: same obiekty Transaction nie są kopiowane. Ponieważ Transaction jest niemodyfikowalnym rekordem, wystarcza to w tym przypadku. List.copyOf zgłosi też NullPointerException, jeśli lista zawiera null.
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