package com.delta.bank.lab.patterns.structural.facade

import com.delta.bank.lab.patterns.structural.decorator.FeeCalculator
import com.delta.bank.lab.patterns.structural.decorator.MinimumFeeDecorator
import com.delta.bank.lab.patterns.structural.decorator.PercentageFeeCalculator
import com.delta.bank.lab.patterns.structural.adapter.PaymentGateway

import spock.lang.Specification

class FacadeStructuralPatternsSpec extends Specification {

    /*
    Fasadę stosuje się po to, żeby dać klientowi (np jakaś usługa lub komponent) prosty punkt wejścia do bardziej złożonego podsystemu.
    Klient nie musi znać kolejności wywołań ani szczegółów współpracy wielu klas — fasada koordynuje je za niego.
    Czyli fasada upraszcza korzystanie z podsystemu, ale nie ukrywa go całkowicie
        — inne klasy nadal mogą używać jego komponentów bezpośrednio, jeśli potrzebują większej kontroli.
    Czyli klient wywołując fasadę nie musi znać szczegółów podsystemu, ale może je poznać i używać bezpośrednio, jeśli chce.
    Różni się od adaptera: adapter dopasowuje niezgodne interfejsy, a fasada upraszcza korzystanie z kilku współpracujących elementów.
    W Labie 42 TransferFacade przyjmuje przelew, sprawdza podstawowe warunki, oblicza opłatę, wywołuje gateway i zwraca potwierdzenie.
    Bez fasady kod wywołujący musiałby sam składać te kroki i znać kilka komponentów.
    Fasada nie musi ukrywać całego podsystemu — inne klasy nadal mogą używać jego komponentów bezpośrednio,
    jeśli potrzebują większej kontroli.
    * */

    // Fasada ma skoordynować obliczenie opłaty i wykonanie przelewu przez bramkę płatniczą gateway.
    // W tym teście TransferFacade dostaje bramkę oraz kalkulator opłat.
    // Dla przelewu 10,00 kalkulator procentowy wylicza 0,10, ale MinimumFeeDecorator podnosi opłatę do minimum 2,00.
    // Test sprawdza, że bramka została wywołana dokładnie raz, opłata wynosi 2,00, a łączna kwota obciążenia to 12,00.
    def "fasada koordynuje obliczanie opłaty i wywołanie bramki płatniczej"() {
        given:
        // Mock(PaymentGateway) tworzy w Spocku atrapę bramki płatniczej.
        def gateway = Mock(PaymentGateway)

        def percentageFeeCalculator = new PercentageFeeCalculator(new BigDecimal("0.01"))

        FeeCalculator fees = new MinimumFeeDecorator(
                percentageFeeCalculator,
                new BigDecimal("2.00"))

        // TransferFacade dostaje bramkę oraz kalkulator opłat.
        // Dla przelewu 10,00 kalkulator procentowy wylicza 0,10, ale MinimumFeeDecorator podnosi opłatę do minimum 2,00.
        // Test sprawdza, że bramka została wywołana dokładnie raz, opłata wynosi 2,00, a łączna kwota obciążenia to 12,00.
        // Nie wykonuje prawdziwego przelewu — pozwala sprawdzić, czy fasada wywołała transfer(...).
        def facade = new TransferFacade(gateway, fees)

        when:
        // Nie wykonuje prawdziwego przelewu — pozwala sprawdzić, czy fasada wywołała transfer(...).
        def receipt = facade.transfer("PLN-1", "PLN-2", new BigDecimal("10.00"))

        then:
        // tutaj:  1 * gateway.transfer(...) oznacza „dokładnie jedno wywołanie”
        1 * gateway.transfer("PLN-1", "PLN-2", new BigDecimal("10.00"))
        receipt.fee() == new BigDecimal("2.00")
        receipt.totalDebit() == new BigDecimal("12.00")
    }

    def "fasada odrzuca nieprawidłowy przelew bez wywoływania bramki"() {
        given:
        // Mock(PaymentGateway) tworzy w Spocku atrapę bramki płatniczej.
        def gateway = Mock(PaymentGateway)
        def facade = new TransferFacade(gateway, (amount) -> BigDecimal.ZERO)

        when:
        // Nie wykonuje prawdziwego przelewu — pozwala sprawdzić, czy fasada wywołała transfer(...).
        facade.transfer("PLN-1", "PLN-1", BigDecimal.TEN)

        then:
        thrown(IllegalArgumentException)
        // Nie wykonuje prawdziwego przelewu — pozwala sprawdzić, czy fasada wywołała transfer(...).
        // tutaj:  0 * ... — „żadnego” oznacza „żadnego wywołania”
        0 * gateway.transfer(_, _, _)
    }
}