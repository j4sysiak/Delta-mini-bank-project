package com.delta.bank.lab.patterns.structural.adapter


import spock.lang.Specification

class AdapterStructuralPatternsSpec extends Specification {

    def "adapter przelicza złote na dokładne grosze dla starszego API"() {
        given:
        def legacy = Mock(LegacyPaymentGateway)
        def adapter = new LegacyPaymentGatewayAdapter(legacy)

        when:
        adapter.transfer("PLN-1", "PLN-2", new BigDecimal("12.34"))

        then:
        1 * legacy.send("PLN-1", "PLN-2", 1234L)
    }

    def "adapter odrzuca ułamki grosza zamiast zaokrąglać"() {
        given:
        def legacy = Mock(LegacyPaymentGateway)
        def adapter = new LegacyPaymentGatewayAdapter(legacy)

        when:
        adapter.transfer("PLN-1", "PLN-2", new BigDecimal("1.001"))

        then:
        thrown(ArithmeticException)
        0 * legacy.send(_, _, _)
    }

}