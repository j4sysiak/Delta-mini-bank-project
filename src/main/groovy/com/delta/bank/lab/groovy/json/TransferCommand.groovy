package com.delta.bank.lab.groovy.json

class TransferCommand {
    final String requestId
    final String fromAccount
    final String toAccount
    final BigDecimal amount

    TransferCommand(
            String requestId,
            String fromAccount,
            String toAccount,
            BigDecimal amount
    ) {
        this.requestId = requestId
        this.fromAccount = fromAccount
        this.toAccount = toAccount
        this.amount = amount
    }
}