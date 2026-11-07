package com.delta.bank.lab.groovy.closures

import spock.lang.Specification

class BankTransactionAnalyzerSpec extends Specification {

    def "findAll wybiera wszystkie transakcje danego rodzaju"() {
        expect:
        analyzer().findByKind(TransactionKind.DEPOSIT)*.accountNumber ==
                ["PLN-1001", "PLN-1002"]

        // *.accountNumber to operator spread-dot w Groovy.
        // Odczytuje accountNumber z każdego elementu kolekcji i zwraca listę tych wartości.
    }

    def "collect tworzy opis każdej transakcji"() {
        expect:
        analyzer().descriptions() == [
                "PLN-1001: DEPOSIT 100.00",
                "PLN-1001: WITHDRAWAL 25.00",
                "PLN-1002: DEPOSIT 10.00",
                "PLN-1001: TRANSFER 40.00"
        ]
    }

    def "find zwraca pierwsze pasujące dopasowanie"() {
        when:
        def result = analyzer().firstAtLeast(
                TransactionKind.DEPOSIT,
                new BigDecimal("50.00")
        )

        then:
        result.accountNumber == "PLN-1001"
        result.amount.compareTo(new BigDecimal("100.00")) == 0
    }

    def "find zwraca null, gdy nie ma pasującej transakcji"() {
        expect:
        analyzer().firstAtLeast(
                TransactionKind.WITHDRAWAL,
                new BigDecimal("50.00")
        ) == null
    }

    def "groupBy tworzy osobną grupę dla każdego rodzaju"() {
        when:
        def groups = analyzer().groupByKind()

        then:
        groups.keySet() == ([
                TransactionKind.DEPOSIT,
                TransactionKind.WITHDRAWAL,
                TransactionKind.TRANSFER
        ] as Set)

        // *.accountNumber to operator spread-dot w Groovy.
        // Odczytuje accountNumber z każdego elementu kolekcji i zwraca listę tych wartości.
        groups[TransactionKind.DEPOSIT]*.accountNumber == ["PLN-1001", "PLN-1002"]

        groups[TransactionKind.WITHDRAWAL].size() == 1
        groups[TransactionKind.TRANSFER].size() == 1
    }

    def "inject sumuje kwoty dla wybranego rodzaju"() {
        expect:
        analyzer().totalFor(TransactionKind.DEPOSIT)
                .compareTo(new BigDecimal("110.00")) == 0

        analyzer().totalFor(TransactionKind.WITHDRAWAL)
                .compareTo(new BigDecimal("25.00")) == 0

        analyzer().totalFor(TransactionKind.TRANSFER)
                .compareTo(new BigDecimal("40.00")) == 0
    }

    def "inject zwraca zero dla pustej listy"() {
        expect:
        new BankTransactionAnalyzer([])
                .totalFor(TransactionKind.DEPOSIT)
                .compareTo(BigDecimal.ZERO) == 0
    }

    def "rejects a null transaction in the input list"() {
        given:
        def transactions = new ArrayList<BankTransaction>()
        transactions.add(null)

        when:
        new BankTransactionAnalyzer(transactions)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "transactions must not contain null"
    }

    private static BankTransactionAnalyzer analyzer() {
        new BankTransactionAnalyzer([
                transaction("PLN-1001", TransactionKind.DEPOSIT, "100.00"),
                transaction("PLN-1001", TransactionKind.WITHDRAWAL, "25.00"),
                transaction("PLN-1002", TransactionKind.DEPOSIT, "10.00"),
                transaction("PLN-1001", TransactionKind.TRANSFER, "40.00")
        ])
    }

    private static BankTransaction transaction(
            String accountNumber,
            TransactionKind kind,
            String amount
    ) {
        new BankTransaction(accountNumber, kind, new BigDecimal(amount))
    }

}