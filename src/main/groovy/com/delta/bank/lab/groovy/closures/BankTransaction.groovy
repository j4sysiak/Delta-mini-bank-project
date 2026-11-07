package com.delta.bank.lab.groovy.closures

class BankTransaction {
    final String accountNumber
    final TransactionKind kind
    final BigDecimal amount

    BankTransaction(
            String accountNumber,
            TransactionKind kind,
            BigDecimal amount
    ) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null")
        Objects.requireNonNull(kind, "kind must not be null")
        Objects.requireNonNull(amount, "amount must not be null")

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank")
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero")
        }

        this.accountNumber = accountNumber
        this.kind = kind
        this.amount = amount
    }
}