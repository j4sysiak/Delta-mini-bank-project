package com.delta.bank.lab.groovy.staticcompilation

import spock.lang.Specification

class TransactionReportSpec extends Specification {

    def "returns amounts for deposits only"() {
        expect:
        report().depositAmounts() == [
                new BigDecimal("100.00"),
                new BigDecimal("25.50")
        ]
    }

    def "sums deposit amounts"() {
        expect:
        report().totalDeposits().compareTo(new BigDecimal("125.50")) == 0
    }

    def "returns zero when there are no deposits"() {
        given:
        def report = new TransactionReport([
                transaction(TransactionKind.WITHDRAWAL, "10.00")
        ])

        expect:
        report.totalDeposits().compareTo(BigDecimal.ZERO) == 0
    }

    def "does not retain the input list"() {
        given:
        def transactions = new ArrayList<BankTransaction>()
        transactions.add(transaction(TransactionKind.DEPOSIT, "10.00"))
        def report = new TransactionReport(transactions)

        when:
        transactions.add(transaction(TransactionKind.DEPOSIT, "20.00"))

        then:
        report.depositAmounts() == [new BigDecimal("10.00")]
    }

    def "rejects a null transaction"() {
        given:
        def transactions = new ArrayList<BankTransaction>()
        transactions.add(null)

        when:
        new TransactionReport(transactions)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "transactions must not contain null"
    }

    private static TransactionReport report() {
        new TransactionReport([
                transaction(TransactionKind.DEPOSIT, "100.00"),
                transaction(TransactionKind.WITHDRAWAL, "10.00"),
                transaction(TransactionKind.DEPOSIT, "25.50")
        ])
    }

    private static BankTransaction transaction(
            TransactionKind kind,
            String amount
    ) {
        new BankTransaction(
                "PLN-1001",
                kind,
                new BigDecimal(amount)
        )
    }
}