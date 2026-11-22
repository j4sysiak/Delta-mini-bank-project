package com.delta.bank.lab.patterns.creational.factory


import spock.lang.Specification

class FactoryPatternsSpec extends Specification {

    /*
    **Factory wybiera, jaki obiekt utworzyć, a Builder pozwala skonfigurować obiekt krok po kroku.**

    W tych testach:

     - Factory: `AccountFactory.create(type, "A-1")` na podstawie `type` tworzy odpowiedni podtyp konta:
         - `Checking`
         - `Savings`
         - `Business`.
       Ukrywa sposób wyboru i tworzenia obiektu.

     - Builder: `TransferRequest.builder().from(...).to(...).amount(...).build()`
       zbiera parametry żądania, a `build()` kończy tworzenie obiektu.
       Niepodane `memo` otrzymuje tutaj wartość domyślną `""`.

     W skrócie: Factory — „utwórz odpowiedni rodzaj konta”;
     Builder — „zbuduj przelew z tymi danymi”.
    * */

    def "fabryka tworzy odpowiedni podtyp"() {
        expect:
        AccountFactory.create(type, "A-1").class == expected

        where:
        type                             || expected
        AccountFactory.Type.CHECKING     || Account.Checking
        AccountFactory.Type.SAVINGS      || Account.Savings
        AccountFactory.Type.BUSINESS     || Account.Business
    }

    def "fabryka odrzuca null"() {
        when:
        AccountFactory.create(null, "A-1")

        then:
        thrown(NullPointerException)
    }
}