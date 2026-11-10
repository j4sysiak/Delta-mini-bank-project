package com.delta.bank.lab.groovy.ast

import groovy.transform.Canonical

@Canonical
class TransactionRecord {
    String accountNumber
    TransactionKind kind
    BigDecimal amount
}