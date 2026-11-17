package com.delta.bank.lab.patterns.behavioral2.state


import spock.lang.Specification

class StatePatternsSpec extends Specification {

    def "płatność przechodzi ze stanu oczekującego do opłaconego"() {
        given:
        def payment = new Payment()

        when:
        payment.pay()

        then:
        payment.status() == "PAID"
    }

    def "płatność oczekująca może zostać anulowana"() {
        given:
        def payment = new Payment()

        when:
        payment.cancel()

        then:
        payment.status() == "CANCELLED"
    }

    def "płatność opłacona nie może zostać opłacona ponownie ani anulowana"() {
        given:
        def payment = new Payment()
        payment.pay()

        when:
        payment.cancel()

        then:
        thrown(IllegalStateException)
        payment.status() == "PAID"
    }

}