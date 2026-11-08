package com.delta.bank.lab.groovy.staticcompilation

import spock.lang.Specification

class TransactionReportSpec extends Specification {

    def "zwraca kwoty tylko dla depozytów"() {
        expect:
        report().depositAmounts() == [
                new BigDecimal("100.00"),
                new BigDecimal("25.50")
        ]
    }

    def "sumuje kwoty depozytów"() {
        expect:
        report().totalDeposits().compareTo(new BigDecimal("125.50")) == 0
    }

    def "zwraca zero, gdy nie ma depozytów"() {
        given:
        def report = new TransactionReport([
                transaction(TransactionKind.WITHDRAWAL, "10.00")
        ])

        expect:
        report.totalDeposits().compareTo(BigDecimal.ZERO) == 0
    }

    /*
    Ten test sprawdza, czy `TransactionReport` nie przechowuje przekazanej listy wejściowej jako własnej.
    Po utworzeniu raportu test dopisuje do oryginalnej listy kolejną transakcję i weryfikuje,
    że raport nadal zwraca tylko pierwotną kwotę `10.00`.
    To potwierdza, że konstruktor kopiuje listę.
    * */
    def "nie zachowuje listy wejściowej"() {
        given:
        def transactions = new ArrayList<BankTransaction>()
        transactions.add(transaction(TransactionKind.DEPOSIT, "10.00"))
        def report = new TransactionReport(transactions)

        when:
        transactions.add(transaction(TransactionKind.DEPOSIT, "20.00"))

        then:
        report.depositAmounts() == [new BigDecimal("10.00")]
    }

    def "odrzuca nullową transakcję"() {
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