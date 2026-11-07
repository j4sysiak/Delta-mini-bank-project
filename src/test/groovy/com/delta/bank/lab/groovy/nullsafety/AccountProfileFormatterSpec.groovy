package com.delta.bank.lab.groovy.nullsafety

import spock.lang.Specification
import spock.lang.Unroll

class AccountProfileFormatterSpec extends Specification {

    private final AccountProfileFormatter formatter =
            new AccountProfileFormatter()

    def "safe navigation reads a nested email"() {
        given:
        def profile = new AccountProfile(
                "Anna Kowalska",
                new AccountContact("anna@example.test"),
                ["premium"]
        )

        expect:
        formatter.emailLabel(profile) == "anna@example.test"
    }

    @Unroll
    def "Elvis supplies an email label for #caseName"() {
        expect:
        formatter.emailLabel(profile) == "No email"

        where:
        caseName             | profile
        // "a null profile"     | null
        "a null contact"     | new AccountProfile("Anna", null, [])
        "a null email"       | new AccountProfile("Anna", new AccountContact(null), [])
        "an empty email"     | new AccountProfile("Anna", new AccountContact(""), [])
    }

    def "owner label strips whitespace before applying its fallback"() {
        expect:
        formatter.ownerLabel(new AccountProfile("  Anna Kowalska  ", null, [])) ==
                "Anna Kowalska"
        formatter.ownerLabel(new AccountProfile("  ", null, [])) ==
                "Unknown owner"
        formatter.ownerLabel(null) == "Unknown owner"
    }

    @Unroll
    def "Groovy Truth determines whether profile has tags: #caseName"() {
        expect:
        formatter.hasTags(profile) == expected

        where:
        caseName          | profile                                      | expected
        "null profile"    | null                                         | false
        "null tags"       | new AccountProfile("Anna", null, null)       | false
        "empty tags"      | new AccountProfile("Anna", null, [])         | false
        "one tag"         | new AccountProfile("Anna", null, ["premium"]) | true
    }

    def "explicit null check preserves zero as a valid limit"() {
        expect:
        formatter.effectiveLimit(
                BigDecimal.ZERO,
                new BigDecimal("100.00")
        ).compareTo(BigDecimal.ZERO) == 0
    }

    def "explicit null check uses the default only for null"() {
        expect:
        formatter.effectiveLimit(
                null,
                new BigDecimal("100.00")
        ).compareTo(new BigDecimal("100.00")) == 0
    }

    def "Groovy Truth treats empty values as false but nonempty strings as true"() {
        expect:
        !truthy(null)
        !truthy(false)
        !truthy(0)
        !truthy(BigDecimal.ZERO)
        !truthy("")
        !truthy([])
        !truthy([:])

        truthy("false")
        truthy("0")
        truthy(["premium"])
    }

    private static boolean truthy(Object value) {
        value ? true : false
    }
}