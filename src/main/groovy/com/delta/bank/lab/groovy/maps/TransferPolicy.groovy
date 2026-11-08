package com.delta.bank.lab.groovy.maps

class TransferPolicy {
    final String accountNumber
    final String currency
    final BigDecimal dailyLimit
    final boolean enabled

    TransferPolicy(
            String accountNumber,
            String currency,
            BigDecimal dailyLimit,
            boolean enabled
    ) {
        this.accountNumber = accountNumber
        this.currency = currency
        this.dailyLimit = dailyLimit
        this.enabled = enabled
    }
}