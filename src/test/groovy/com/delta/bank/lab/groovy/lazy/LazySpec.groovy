package com.delta.bank.lab.groovy.lazy

import spock.lang.Specification

class LazySpec extends Specification {

    def "memoized zwraca ten sam wynik i oblicza raz na argument"() {
        given:
        def calc = new FeeCalculator()

        when:
        def first = calc.feeFor(new BigDecimal("100.00"))
        def second = calc.feeFor(new BigDecimal("100.00"))

        then:
        first == new BigDecimal("1.00")
        second.is(first)
        calc.calls == 1
    }

    def "różne argumenty są obliczane osobno"() {
        given:
        def calc = new FeeCalculator()

        when:
        calc.feeFor(new BigDecimal("100.00"))
        calc.feeFor(new BigDecimal("200.00"))

        then:
        calc.calls == 2
    }

    def "maxCacheSize usuwa najstarsze wpisy z pamięci podręcznej"() {
        given:
        def calc = new FeeCalculator()

        when:
        calc.label("A")
        calc.label("B")
        calc.label("C")
        calc.label("A")

        then:
        calc.calls == 4
    }

    def "pole lazy ładuje się tylko przy pierwszym odczycie"() {
        given:
        def cache = new StatementCache()

        expect:
        cache.loads == 0

        when:
        def first = cache.lines
        def second = cache.lines

        then:
        first.is(second)
        cache.loads == 1
    }
}