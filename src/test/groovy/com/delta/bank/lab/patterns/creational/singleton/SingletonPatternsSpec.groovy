package com.delta.bank.lab.patterns.creational.singleton


import com.delta.bank.lab.patterns.creational.IdGenerator
import spock.lang.Specification

class SingletonPatternsSpec extends Specification {

    def "enum singleton jest jedną instancją i generuje unikalne identyfikatory"() {
        expect:
        IdGenerator.INSTANCE.is(IdGenerator.valueOf("INSTANCE"))

        when:
        def first  = IdGenerator.INSTANCE.next("TX")
        def second = IdGenerator.INSTANCE.next("TX")

        then:
        first != second
        first.startsWith("TX-")
    }
}