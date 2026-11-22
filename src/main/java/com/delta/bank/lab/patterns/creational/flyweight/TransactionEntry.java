package com.delta.bank.lab.patterns.creational.flyweight;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Przedstawia jedno wpisane zdarzenie transakcyjne.
 * Klasa przechowuje stan zewnętrzny, który nie jest współdzielony między transakcjami.
 *
 * @param type        typ transakcji
 * @param description opis transakcji
 * @param amount      kwota transakcji
 */
public record TransactionEntry(
        TransactionType type,
        String description,
        BigDecimal amount) {

    public TransactionEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
    }
}