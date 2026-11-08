package com.delta.bank.lab.groovy.maps

import spock.lang.Specification
import spock.lang.Unroll

class TransferPolicyFactorySpec extends Specification {

    private final TransferPolicyFactory factory = new TransferPolicyFactory()

    def "argumenty nazwane tworzą polisę"() {
        when:
        def policy = factory.create(
                accountNumber: "PLN-1001",
                currency: "PLN",
                dailyLimit: new BigDecimal("250.00"),
                enabled: true
        )

        then:
        policy.accountNumber == "PLN-1001"
        policy.currency == "PLN"
        policy.dailyLimit.compareTo(new BigDecimal("250.00")) == 0
        policy.enabled
    }

    def "używa wartości domyślnych tylko dla pominiętych opcjonalnych kluczy"() {
        when:
        def policy = factory.create(
                accountNumber: "PLN-1001",
                currency: "PLN"
        )

        then:
        policy.dailyLimit.compareTo(new BigDecimal("1000.00")) == 0
        policy.enabled
    }

    def "zachowuje false i zero zamiast używania wartości domyślnych"() {
        when:
        def policy = factory.create(
                accountNumber: "PLN-1001",
                currency: "PLN",
                dailyLimit: BigDecimal.ZERO,
                enabled: false
        )

        then:
        policy.dailyLimit.compareTo(BigDecimal.ZERO) == 0
        !policy.enabled
    }

    def "pozwala na składanie opcji z map"() {
        given:
        def required = [
                accountNumber: "PLN-1001",
                currency: "PLN"
        ]
        def overrides = [
                dailyLimit: new BigDecimal("75.00"),
                enabled: false
        ]

        when:
        def policy = factory.create(required + overrides)

        then:
        policy.dailyLimit.compareTo(new BigDecimal("75.00")) == 0
        !policy.enabled
    }

    def "odrzuca brakujący wymagany klucz"() {
        when:
        factory.create(currency: "PLN")

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "accountNumber is required"
    }

    @Unroll
    def "odrzuca nieprawidłową opcję #caseName"() {
        when:
        factory.create(options)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == expectedMessage

        where:
        caseName            | options                                                        | expectedMessage
        "null account"      | [accountNumber: null, currency: "PLN"]                         | "accountNumber must be a String"
        "blank account"     | [accountNumber: "  ", currency: "PLN"]                         | "accountNumber must not be blank"
        "lowercase currency"| [accountNumber: "PLN-1001", currency: "pln"]                   | "currency must be a 3-letter uppercase code"
        "null limit"        | [accountNumber: "PLN-1001", currency: "PLN", dailyLimit: null] | "dailyLimit must be a BigDecimal"
        "negative limit"    | [accountNumber: "PLN-1001", currency: "PLN", dailyLimit: new BigDecimal("-1.00")] | "dailyLimit must not be negative"
        "string limit"      | [accountNumber: "PLN-1001", currency: "PLN", dailyLimit: "100.00"] | "dailyLimit must be a BigDecimal"
        "null enabled"      | [accountNumber: "PLN-1001", currency: "PLN", enabled: null]    | "enabled must be a Boolean"
        "unknown option"    | [accountNumber: "PLN-1001", currency: "PLN", enabeld: true]    | "Unknown option(s): enabeld"
    }

    def "odrzuca null mapę opcji"() {
        when:
        factory.create(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "options must not be null"
    }
}