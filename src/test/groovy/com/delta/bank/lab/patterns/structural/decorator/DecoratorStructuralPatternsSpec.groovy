package com.delta.bank.lab.patterns.structural.decorator


import spock.lang.Specification

class DecoratorStructuralPatternsSpec extends Specification {

    // Dekorator opakowuje istniejący obiekt (komponent),
    // U nas tym istniejącym obiektem (komponentem) jest percentage — instancja PercentageFeeCalculator.
    // MinimumFeeDecorator ją opakowuje, a zmienna calculator wskazuje już na dekorator:
    // dodając mu nowe zachowanie bez zmieniania jego podstawowej logiki.


    // FeeCalculator to wspólny typ (u nas interfejs) dla obiektów obliczających opłatę.
    // FeeCalculator to po prostu u nas wspólna „umowa”: każdy obiekt tego typu potrafi obliczyć opłatę:
    //  - PercentageFeeCalculator oblicza opłatę jako procent kwoty,
    //  - MinimumFeeDecorator opakowuje inny FeeCalculator (czyli percentage) i pilnuje, by wynik nie był mniejszy niż określona minimalna opłata,
    //     i nadal działa jak zwykły kalkulator opłat
    //     Dzięki temu można go używać tak samo jak wcześniejszego kalkulatora.
    def "dekorator minimalnej opłaty podnosi opłatę poniżej minimum"() {
        given:
        // Naszym obiektem (komponentem) jest percentage — instancja PercentageFeeCalculator.
        // gdzie, w PercentageFeeCalculator obliczamy opłatę jako 1% kwoty.
        FeeCalculator percentage = new PercentageFeeCalculator(new BigDecimal("0.01"))

        // MinimumFeeDecorator opakowuje obiekt `percentage` i pilnuje, by wynik nie był mniejszy niż 2.00.
        FeeCalculator calculator = new MinimumFeeDecorator(percentage, new BigDecimal("2.00"))

        expect:
        // Dla 10.00 opłata procentowa wynosi 0.10, więc dekorator podnosi ją do 2.00.
        // Dzięki temu calculator nadal używa interfejsu FeeCalculator, ale ma dodatkową regułę minimalnej opłaty.
        calculator.calculate(new BigDecimal("10.00")) == new BigDecimal("2.00")
    }

    def "dekorator minimalnej opłaty zachowuje większą obliczoną opłatę"() {
        given:
        FeeCalculator percentage = new PercentageFeeCalculator(new BigDecimal("0.01"))

        // MinimumFeeDecorator opakowuje obiekt `percentage` i pilnuje, by wynik nie był mniejszy niż 2.00.
        FeeCalculator calculator = new MinimumFeeDecorator(percentage, new BigDecimal("2.00"))

        expect:
        // Dla 500.00 wynosi 5.00, więc dekorator pozostawia ją bez zmian.
        calculator.calculate(new BigDecimal("500.00")) == new BigDecimal("5.00")
    }

}