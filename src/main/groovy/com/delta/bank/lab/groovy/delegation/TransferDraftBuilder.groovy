package com.delta.bank.lab.groovy.delegation

class TransferDraftBuilder {
    String fromAccount
    String toAccount
    BigDecimal amount
    String memo = ""

    TransferDraft build() {
        if (fromAccount == null || fromAccount.isBlank()) {
            throw new IllegalArgumentException("fromAccount is required")
        }
        if (toAccount == null || toAccount.isBlank()) {
            throw new IllegalArgumentException("toAccount is required")
        }
        if (fromAccount == toAccount) {
            throw new IllegalArgumentException(
                    "fromAccount and toAccount must be different"
            )
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required")
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero")
        }
        if (memo == null) {
            throw new IllegalArgumentException("memo must not be null")
        }
        return new TransferDraft(fromAccount, toAccount, amount, memo)
    }
}