package com.delta.bank.lab.groovy.regex

import spock.lang.Specification
import spock.lang.Unroll

class TransferTextParserSpec extends Specification {

    private final TransferTextParser parser = new TransferTextParser()

    // @Unroll w Spocku sprawia, że każdy wiersz z sekcji where: jest uruchamiany i wyświetlany jako osobny test.
    @Unroll
    def "==~ validates the entire account number: #value"() {
        expect:
        parser.isAccountNumber(value) == valid

        where:
        value            | valid
        "PLN-1001"       | true
        "EUR-0007"       | true
        "Account PLN-1001" | false
        "pln-1001"       | false
        "PLN-100"        | false
        "PLN-10010"      | false
        ""               | false
        null             | false
    }

    def "=~ finds account numbers inside a larger text"() {
        expect:
        parser.findAccountNumbers(
                "Transfer PLN-1001 -> EUR-2002; invalid XPLN-3003 and PLN-40000"
        ) == ["PLN-1001", "EUR-2002"]
    }

    def "returns an empty list when there are no account numbers"() {
        expect:
        parser.findAccountNumbers("No account references here") == []
    }

    def "parses named groups into transfer routes"() {
        when:
        def routes = parser.parseTransfers(
                "Audit: TRANSFER PLN-1001 -> EUR-2002; amount=125.50"
        )

        then:
        routes.size() == 1
        routes[0].fromAccount == "PLN-1001"
        routes[0].toAccount == "EUR-2002"
        routes[0].amount.compareTo(new BigDecimal("125.50")) == 0
    }

    def "finds multiple transfers in one text"() {
        when:
        def routes = parser.parseTransfers(
                "TRANSFER PLN-1001 -> EUR-2002; amount=10.00\n" +
                        "TRANSFER EUR-2002 -> USD-3003; amount=7"
        )

        then:
        routes*.amount == [
                new BigDecimal("10.00"),
                new BigDecimal("7")
        ]
    }

    @Unroll
    def "does not parse malformed transfer amount: #amount"() {
        expect:
        parser.parseTransfers(
                "TRANSFER PLN-1001 -> EUR-2002; amount=${amount}"
        ).isEmpty()

        where:
        amount << ["", "-1.00", "1.234", "1."]
    }

    def "rejects null input text"() {
        when:
        parser.findAccountNumbers(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "text must not be null"
    }
}