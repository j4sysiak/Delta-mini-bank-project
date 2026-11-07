package com.delta.bank.lab.groovy.staticcompilation

import groovy.transform.CompileStatic

@CompileStatic
class BankTransaction {
    final String accountNumber
    final TransactionKind kind
    final BigDecimal amount

    BankTransaction(
            String accountNumber,
            TransactionKind kind,
            BigDecimal amount
    ) {
        this.accountNumber = Objects.requireNonNull(
                accountNumber,
                "accountNumber must not be null"
        )
        this.kind = Objects.requireNonNull(kind, "kind must not be null")
        this.amount = Objects.requireNonNull(amount, "amount must not be null")

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank")
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero")
        }
    }
}