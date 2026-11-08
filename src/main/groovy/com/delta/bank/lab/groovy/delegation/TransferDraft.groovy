package com.delta.bank.lab.groovy.delegation

class TransferDraft {
    final String fromAccount
    final String toAccount
    final BigDecimal amount
    final String memo

    TransferDraft(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String memo
    ) {
        this.fromAccount = fromAccount
        this.toAccount = toAccount
        this.amount = amount
        this.memo = memo
    }
}