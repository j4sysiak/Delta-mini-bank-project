package com.delta.bank.lab.groovy.gpath

import spock.lang.Specification

class TransactionReportReaderSpec extends Specification {

    private final TransactionReportReader reader =
            new TransactionReportReader()

    def "reads a nested account number with GPath"() {
        expect:
        reader.accountNumber(reader.read(reportJson())) == "PLN-1001"
    }

    def "collects transaction types through GPath"() {
        expect:
        reader.transactionTypes(reader.read(reportJson())) ==
                ["DEPOSIT", "WITHDRAWAL", "DEPOSIT"]
    }

    def "filters deposits and converts their amounts to BigDecimal"() {
        when:
        def amounts = reader.depositAmounts(reader.read(reportJson()))

        then:
        amounts.size() == 2
        amounts[0].compareTo(new BigDecimal("100.00")) == 0
        amounts[1].compareTo(new BigDecimal("25.50")) == 0
    }

    def "rejects a non-object JSON root"() {
        when:
        reader.read("[]")

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "JSON root must be an object"
    }

    def "rejects a missing account object"() {
        when:
        reader.accountNumber([:])

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "account must be an object"
    }

    def "rejects an invalid transactions collection"() {
        when:
        reader.transactionTypes([transactions: "not-an-array"])

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "transactions must be an array"
    }

    def "rejects a non-object transaction element"() {
        when:
        reader.transactionTypes([transactions: ["not-an-object"]])

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "each transaction must be an object"
    }

    def "rejects a non-numeric deposit amount"() {
        given:
        def document = [
                transactions: [
                        [type: "DEPOSIT", amount: "100.00"]
                ]
        ]

        when:
        reader.depositAmounts(document)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "transaction.amount must be a JSON number"
    }

    private static String reportJson() {
        '''
        {
          "account": {
            "number": "PLN-1001",
            "owner": "Anna Kowalska"
          },
          "transactions": [
            {"type": "DEPOSIT", "amount": 100.00},
            {"type": "WITHDRAWAL", "amount": 20.00},
            {"type": "DEPOSIT", "amount": 25.50}
          ]
        }
        '''
    }
}