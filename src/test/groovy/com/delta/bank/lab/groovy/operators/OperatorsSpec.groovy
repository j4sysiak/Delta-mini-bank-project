package com.delta.bank.lab.groovy.operators

import spock.lang.Specification

class OperatorsSpec extends Specification {

    def "zakresy mogą obejmować lub wykluczać koniec"() {
        expect:
        (1..5).toList() == [1, 2, 3, 4, 5]
        (1..<5).toList() == [1, 2, 3, 4]
        (5..1).toList() == [5, 4, 3, 2, 1]
        ('a'..'d').toList() == ['a', 'b', 'c', 'd']
    }

    def "zakresy wycinają listy i ciągi znaków"() {
        given:
        def list = [10, 20, 30, 40, 50]

        expect:
        list[1..3] == [20, 30, 40]
        list[-2..-1] == [40, 50]
        "statement"[0..3] == "stat"
    }

    def "operator spread wywołuje właściwość na każdym elemencie"() {
        given:
        def owners = [[name: "Ann"], [name: "Bob"]]

        expect:
        owners*.name == ["Ann", "Bob"]
        [1, 2, 3]*.multiply(2) == [2, 4, 6]
    }

    def "spread rozwija listę na argumenty"() {
        /*
        To jest **closure** (funkcja) w Groovy przypisana do zmiennej `add`.
        Przyjmuje trzy argumenty (`a`, `b`, `c`) i zwraca ich sumę.
        * */
        given:
        def add = { a, b, c -> a + b + c }

        // Tutaj `add(*[1, 2, 3])` operator `*` rozwija listę do osobnych argumentów, więc wynik to `6`.
        expect:
        add(*[1, 2, 3]) == 6
        // [0, *[1, 2], 3] tworzy nową listę i rozwija elementy zagnieżdżonej listy [1, 2] do jej środka.
        [0, *[1, 2], 3] == [0, 1, 2, 3]
    }

    def "operator porównania kosmicznego"() {
        expect:
        // To operator „spaceship” (<=>) w Groovy — porównuje wartości i zwraca liczbę ujemną, 0 albo dodatnią.
        // Tutaj 1 jest mniejsze od 2, więc wynik to -1.
        (1 <=> 2) == -1
        (2 <=> 2) == 0
        ("b" <=> "a") == 1
    }

    def "operatory mapują się na metody"() {
        given:
        def a = new Money(new BigDecimal("10.00"), "PLN")
        def b = new Money(new BigDecimal("2.50"), "PLN")

        expect:
        (a + b).amount == new BigDecimal("12.50")
        (a - b).amount == new BigDecimal("7.50")
        (b * 2).amount == new BigDecimal("5.00")
        (-a).amount == new BigDecimal("-10.00")
        a > b
        b < a
    }

    def "different currencies are rejected"() {
        when:
        new Money(1.0, "PLN") + new Money(1.0, "EUR")

        then:
        thrown(IllegalArgumentException)
    }

    def "in uses isCase and as converts"() {
        expect:
        3 in (1..5)
        !(9 in (1..5))
        "12" as Integer == 12
        [1, 2, 2] as Set == [1, 2] as Set
    }
}