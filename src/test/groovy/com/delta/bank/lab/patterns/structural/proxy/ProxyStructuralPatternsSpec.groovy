package com.delta.bank.lab.patterns.structural.proxy

import spock.lang.Specification
import com.delta.bank.lab.patterns.structural.adapter.PaymentGateway

class ProxyStructuralPatternsSpec extends Specification {

    def "serwer pośredniczący przekazuje dozwolony przelew"() {
        given:
        def paymentGatewayDelegate = Mock(PaymentGateway)
        def proxy = new TransferLimitProxy(paymentGatewayDelegate, new BigDecimal("100.00"))

        when:
        proxy.transfer("PLN-1", "PLN-2", new BigDecimal("50.00"))

        then:
        1 * paymentGatewayDelegate.transfer("PLN-1", "PLN-2", new BigDecimal("50.00"))
    }

    def "serwer pośredniczący blokuje przelew przekraczający limit przed delegowaniem"() {
        given:
        def paymentGatewayDelegate = Mock(PaymentGateway)
        def proxy = new TransferLimitProxy(paymentGatewayDelegate, new BigDecimal("100.00"))

        when:
        proxy.transfer("PLN-1", "PLN-2", new BigDecimal("100.01"))

        then:
        thrown(IllegalArgumentException)
        0 * paymentGatewayDelegate.transfer(_, _, _)
    }

}