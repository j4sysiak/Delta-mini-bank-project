package com.delta.bank.lab.groovy.staticcompilation

import groovy.transform.CompileStatic

@CompileStatic
class TransactionReport {

    private final List<BankTransaction> transactions

    TransactionReport(List<BankTransaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null")

        if (transactions.any { BankTransaction tx -> tx == null }) {
            throw new IllegalArgumentException("transactions must not contain null")
        }

        this.transactions = List.copyOf(transactions)
    }

    List<BigDecimal> depositAmounts() {
        return transactions
                .findAll { BankTransaction tx ->
                    tx.kind == TransactionKind.DEPOSIT
                }
                .collect { BankTransaction tx ->
                    tx.amount
                }
    }

    BigDecimal totalDeposits() {
        BigDecimal total = BigDecimal.ZERO

        for (BankTransaction tx : transactions) {
            if (tx.kind == TransactionKind.DEPOSIT) {
                total = total.add(tx.amount)
            }
        }

        return total
    }
}