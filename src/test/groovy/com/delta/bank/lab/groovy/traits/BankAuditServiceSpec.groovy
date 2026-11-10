package com.delta.bank.lab.groovy.traits

import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class BankAuditServiceSpec extends Specification {

    private static Clock fixedClock() {
        Clock.fixed(
                Instant.parse("2026-03-04T10:15:30Z"),
                ZoneOffset.UTC
        )
    }

    def "korzysta z zachowania traitu oraz zegara i źródła implementacji"() {
        given:
        def service = new BankAuditService(fixedClock(), "transfer-service")

        expect:
        service.auditLine(" Transfer PLN-1001 -> PLN-1002 ") ==
                "2026-03-04T10:15:30|transfer-service|Transfer PLN-1001 -> PLN-1002"
    }

    def "to samo zachowanie traitu może być ponownie użyte z innym źródłem"() {
        given:
        def service = new BankAuditService(fixedClock(), "deposit-service")

        expect:
        service.auditLine("Deposit PLN-1001") ==
                "2026-03-04T10:15:30|deposit-service|Deposit PLN-1001"
    }

    def "odrzuca null jako akcję"() {
        given:
        def service = new BankAuditService(fixedClock(), "transfer-service")

        when:
        service.auditLine(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "action must not be null"
    }

    def "odrzuca pustą akcję"() {
        given:
        def service = new BankAuditService(fixedClock(), "transfer-service")

        when:
        service.auditLine(" \t ")

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "action must not be blank"
    }

    def "odrzuca pusty audit source"() {
        given:
        def service = new BankAuditService(fixedClock(), " ")

        when:
        service.auditLine("Deposit PLN-1001")

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "auditSource must not be blank"
    }

    def "odrzuca null jako zegar w klasie implementującej"() {
        when:
        new BankAuditService(null, "transfer-service")

        then:
        def exception = thrown(NullPointerException)
        exception.message == "clock must not be null"
    }
}