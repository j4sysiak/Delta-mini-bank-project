package com.delta.bank.lab.groovy.operators

class Money implements Comparable<Money> {

    final BigDecimal amount
    final String currency

    Money(BigDecimal amount, String currency) {
        this.amount = amount
        this.currency = currency
    }

    Money plus(Money other) {
        requireSameCurrency(other)
        new Money(amount + other.amount, currency)
    }

    Money minus(Money other) {
        requireSameCurrency(other)
        new Money(amount - other.amount, currency)
    }

    Money multiply(Number factor) {
        new Money(amount * factor, currency)
    }

    Money negative() {
        new Money(-amount, currency)
    }

    @Override
    int compareTo(Money other) {
        requireSameCurrency(other)
        amount <=> other.amount
    }

    private void requireSameCurrency(Money other) {
        if (currency != other.currency) {
            throw new IllegalArgumentException("Currency mismatch: $currency vs $other.currency")
        }
    }

    @Override
    String toString() { "$amount $currency" }
}