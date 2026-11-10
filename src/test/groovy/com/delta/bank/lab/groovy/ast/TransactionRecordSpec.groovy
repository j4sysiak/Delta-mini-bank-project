package com.delta.bank.lab.groovy.ast

import spock.lang.Specification

class TransactionRecordSpec extends Specification {

    def "canonical constructor assigns all properties"() {
        when:
        def transaction = new TransactionRecord(
                "PLN-1001",
                TransactionKind.DEPOSIT,
                new BigDecimal("25.00")
        )

        then:
        transaction.accountNumber == "PLN-1001"
        transaction.kind == TransactionKind.DEPOSIT
        transaction.amount == new BigDecimal("25.00")
    }

    def "canonical equality compares property values"() {
        given:
        def first = transaction("25.00")
        def second = transaction("25.00")

        expect:
        first == second
        first.hashCode() == second.hashCode()
    }

    def "different properties make transactions unequal"() {
        expect:
        transaction("25.00") != new TransactionRecord(
                "PLN-2002",
                TransactionKind.DEPOSIT,
                new BigDecimal("25.00")
        )

        transaction("25.00") != new TransactionRecord(
                "PLN-1001",
                TransactionKind.WITHDRAWAL,
                new BigDecimal("25.00")
        )
    }

    def "toString includes the class and property values"() {
        when:
        def text = transaction("25.00").toString()

        then:
        text.contains("TransactionRecord")
        text.contains("PLN-1001")
        text.contains("DEPOSIT")
        text.contains("25.00")
    }

    def "canonical equality uses Groovy numeric comparison for BigDecimal"() {
        expect:
        transaction("25.0") == transaction("25.00")
        !new BigDecimal("25.0").equals(new BigDecimal("25.00"))
        new BigDecimal("25.0").compareTo(new BigDecimal("25.00")) == 0
    }

    def "canonical equals and hashCode need care with scaled BigDecimal values"() {
        given:
        def oneDecimal = transaction("25.0")
        def twoDecimals = transaction("25.00")

        expect:
        oneDecimal == twoDecimals
        oneDecimal.hashCode() != twoDecimals.hashCode()
    }

    def "canonical object is still mutable by default"() {
        given:
        def transaction = transaction("25.00")

        when:
        transaction.amount = new BigDecimal("30.00")

        then:
        transaction.amount == new BigDecimal("30.00")
    }

    private static TransactionRecord transaction(String amount) {
        new TransactionRecord(
                "PLN-1001",
                TransactionKind.DEPOSIT,
                new BigDecimal(amount)
        )
    }
}