package com.delta.bank.lab.groovy.regex

class TransferRoute {
    final String fromAccount
    final String toAccount
    final BigDecimal amount

    TransferRoute(String fromAccount, String toAccount, BigDecimal amount) {
        this.fromAccount = fromAccount
        this.toAccount = toAccount
        this.amount = amount
    }
}