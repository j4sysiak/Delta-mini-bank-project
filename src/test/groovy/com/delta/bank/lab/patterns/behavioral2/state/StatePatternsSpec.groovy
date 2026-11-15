package com.delta.bank.lab.patterns.behavioral2.state


import spock.lang.Specification

class StatePatternsSpec extends Specification {

    def "payment transitions from pending to paid"() {
        given:
        def payment = new Payment()

        when:
        payment.pay()

        then:
        payment.status() == "PAID"
    }

    def "pending payment can be cancelled"() {
        given:
        def payment = new Payment()

        when:
        payment.cancel()

        then:
        payment.status() == "CANCELLED"
    }

    def "paid payment cannot be paid again or cancelled"() {
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