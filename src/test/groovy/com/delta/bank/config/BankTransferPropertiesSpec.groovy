package com.delta.bank.config

import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import spock.lang.Specification

class BankTransferPropertiesSpec extends Specification {

    // Ta klasa testowa udostępnia bean BankTransferProperties i umożliwia testowanie wiązania oraz walidacji konfiguracji.
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(BankTransferProperties)
    static class PropertiesTestConfiguration {
    }

    // Ten fragment tworzy izolowany kontekst testowy z konfiguracją właściwości oraz jawnie włącza walidację Spring Boot.
    // Dzięki temu testy sprawdzają zarówno wiązanie wartości, jak i odrzucanie niepoprawnej konfiguracji przy starcie kontekstu.
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesTestConfiguration)
                    .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration))

    def "binds YAML-style property names to typed values"() {
        when:
        contextRunner
                // tu jest plik application.yml, który zawiera wartości konfiguracyjne dla właściwości bank.transfer.max-amount i bank.transfer.default-currency.
                // C:\dev\Delta-mini-bank-project\src\main\resources\application.yml
                .withPropertyValues(
                        "bank.transfer.max-amount=2500.75",
                        "bank.transfer.default-currency=EUR")
                .run { context ->
                    assert context.startupFailure == null
                    def properties = context.getBean(BankTransferProperties)
                    assert properties.maxAmount == new BigDecimal("2500.75")
                    assert properties.defaultCurrency == "EUR"
                }

        then:
        noExceptionThrown()
    }


    // Test weryfikuje domyślne wartości właściwości:
    //   - bank.transfer.max-amount = 5000.00
    //   - bank.transfer.default-currency = PLN

    // W tym teście contextRunner.run(...) nie używa .withPropertyValues(...),
    // więc aplikacja nie ma ustawionych tych właściwości
    // i klasy BankTransferProperties powinny zastosować własne domyślne wartości — stąd 5000.00 i PLN.
    def "uses defaults when configuration values are absent"() {
        when:
        contextRunner.run { context ->
            assert context.startupFailure == null
            def properties = context.getBean(BankTransferProperties)
            assert properties.maxAmount == new BigDecimal("5000.00")
            assert properties.defaultCurrency == "PLN"
        }

        then:
        noExceptionThrown()
    }

    def "invalid configuration prevents the context from starting"() {
        when:
        contextRunner
                .withPropertyValues(
                        "bank.transfer.max-amount=0",
                        "bank.transfer.default-currency=pln")
                .run { context ->
                    assert context.startupFailure != null
                }

        then:
        noExceptionThrown()
    }


}