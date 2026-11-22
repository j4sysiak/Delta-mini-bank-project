package com.delta.bank.lab.patterns.creational.flyweight

import spock.lang.Specification

class FlyweightSpec extends Specification {

    def "fabryka zwraca ten sam niezmienny obiekt Flyweight dla danego typu"() {
        given:
        def factory = new TransactionStyleFactory()

        when:
        def first = factory.styleFor(TransactionType.DEPOSIT)
        def second = factory.styleFor(TransactionType.DEPOSIT)

        then:
        first.is(second)
        first == new TransactionStyle("Deposit", "+")
    }

    def "różne typy transakcji otrzymują różne style"() {
        given:
        def factory = new TransactionStyleFactory()

        expect:
        !factory.styleFor(TransactionType.DEPOSIT)
                .is(factory.styleFor(TransactionType.WITHDRAWAL))
    }

    def "renderer współdzieli styl, ale używa zewnętrznego stanu każdej pozycji"() {
        given:
        def styles = new TransactionStyleFactory()
        def renderer = new StatementRenderer(styles)
        def first = new TransactionEntry(
                TransactionType.DEPOSIT, "salary", new BigDecimal("1000.00"))
        def second = new TransactionEntry(
                TransactionType.DEPOSIT, "refund", new BigDecimal("25.00"))

        expect:
        renderer.render(first) == "+ Deposit: salary (1000.00)"
        renderer.render(second) == "+ Deposit: refund (25.00)"
        styles.styleFor(first.type()).is(styles.styleFor(second.type()))
    }

    def "renderAll zachowuje kolejność i zwraca niemodyfikowalny wynik."() {
        given:
        def renderer = new StatementRenderer(new TransactionStyleFactory())
        def entries = [
                new TransactionEntry(TransactionType.DEPOSIT, "salary", new BigDecimal("100.00")),
                new TransactionEntry(TransactionType.WITHDRAWAL, "rent", new BigDecimal("40.00"))
        ]

        when:
        def lines = renderer.renderAll(entries)

        then:
        lines == [
                "+ Deposit: salary (100.00)",
                "- Withdrawal: rent (40.00)"
        ]

        when:
        lines.add("another line")

        then:
        thrown(UnsupportedOperationException)
    }

    def "fabryka odrzuca null jako typ"() {
        when:
        new TransactionStyleFactory().styleFor(null)

        then:
        thrown(NullPointerException)
    }
}