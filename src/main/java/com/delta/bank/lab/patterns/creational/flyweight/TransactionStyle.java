package com.delta.bank.lab.patterns.creational.flyweight;

import java.util.Objects;

// TransactionStyle jest współdzielonym stanem wewnętrznym.
public record TransactionStyle(String label, String marker) {

    public TransactionStyle {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(marker, "marker");
    }

    // Flyweight nie przechowuje opisu ani kwoty pojedynczej transakcji.
    // Metoda render dostaje je jako stan zewnętrzny.
    public String render(String description, String amount) {
        return marker + " " + label + ": " + description + " (" + amount + ")";
    }
}