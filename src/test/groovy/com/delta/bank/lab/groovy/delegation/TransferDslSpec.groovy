package com.delta.bank.lab.groovy.delegation

import spock.lang.Specification

class TransferDslSpec extends Specification {

    // To jest DSL (Domain-Specific Language, język dziedzinowy) — tutaj ma ułatwiać opisywanie przelewów w czytelnej formie.
    // Dzięki niemu można później napisać dsl.transfer { ... } i skonfigurować przelew wewnątrz bloku.
    // Ta linia tworzy obiekt TransferDsl, którego testy używają do wywoływania dsl.transfer { ... }.
    // Dzięki temu nie trzeba tworzyć nowego buildera w każdym teście.
    private final TransferDsl dsl = new TransferDsl()

    String fromAcccount = "owner value"

    def "przekazuje domknięcie konfigurujące do buildera"() {
        when:
        // przekazuje blok { ... } jako argument typu Closure.
        def draft = dsl.transfer {  // Wywołanie DSL, które przekazuje domknięcie konfigurujące przelew.
            fromAccount = "PLN-1001"
            toAccount = "PLN-1002"
            amount = new BigDecimal("25.00")
            memo = "rent"
        }   // to Closure w Groovy, przekazywana jako argument do dsl.transfer.
            // Groovy pozwala zapisać ostatni argument typu Closure poza nawiasami, więc:
            /*
            dsl.transfer {
                fromAccount = "PLN-1001"
                toAccount = "PLN-1002"
                amount = new BigDecimal("25.00")
                memo = "rent"
            }

            jest równoważne z:

            dsl.transfer({
                fromAccount = "PLN-1001"
                toAccount = "PLN-1002"
                amount = new BigDecimal("25.00")
                memo = "rent"
            })
            */

        then:
        draft.fromAccount == "PLN-1001"
        draft.toAccount == "PLN-1002"
        draft.amount.compareTo(new BigDecimal("25.00")) == 0
        draft.memo == "rent"
    }

    def "używa pustego memo, gdy jest pominięte"() {
        when:
        def draft = dsl.transfer {
            fromAccount = "PLN-1001"
            toAccount = "PLN-1002"
            amount = new BigDecimal("1.00")
        }

        then:
        draft.memo == ""
    }

    // Ten test ma sprawdzić, czy dsl.transfer(...) nie zmienia delegata ani strategii rozwiązywania nazw oryginalnego domknięcia (specification).
    // Delegat wskazuje obiekt, na którym Groovy szuka właściwości, takich jak fromAccount;
    // strategia określa kolejność szukania, np. najpierw u delegata czy u właściciela domknięcia.
    def "nie zmienia oryginalnego delegata ani strategii domknięcia"() {
        given:
        Closure<?> specification = {
            fromAccount = "PLN-1001"
            toAccount = "PLN-1002"
            amount = new BigDecimal("10.00")
        }
        def originalDelegate = specification.delegate
        def originalStrategy = specification.resolveStrategy

        when:
        dsl.transfer(specification)

        then:
        specification.delegate.is(originalDelegate)
        specification.resolveStrategy == originalStrategy
    }

    def "nie udaje się dla nieznanej właściwości zamiast używać właściciela"() {
        when:
        dsl.transfer {
            fromAcccount = "PLN-1001"
            toAccount = "PLN-1002"
            amount = new BigDecimal("10.00")
        }

        then:
        thrown(MissingPropertyException)
        fromAcccount == "owner value"
    }

    def "odrzuca przelew na to samo konto"() {
        when:
        dsl.transfer {
            fromAccount = "PLN-1001"
            toAccount = "PLN-1001"
            amount = new BigDecimal("10.00")
        }

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "fromAccount and toAccount must be different"
    }

    def "odrzuca nieujemną kwotę"() {
        when:
        dsl.transfer {
            fromAccount = "PLN-1001"
            toAccount = "PLN-1002"
            amount = BigDecimal.ZERO
        }

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "amount must be greater than zero"
    }

    def "odrzuca niekompletny przelew"() {
        when:
        dsl.transfer {
            fromAccount = "PLN-1001"
            amount = new BigDecimal("10.00")
        }

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "toAccount is required"
    }
}