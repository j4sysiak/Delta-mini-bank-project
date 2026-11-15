package com.delta.bank.lab.patterns.creational.builder


import com.delta.bank.lab.patterns.creational.TransferRequest
import spock.lang.Specification

class BuilderPatternsSpec extends Specification {

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

    def "builder tworzy poprawne żądanie z domyślnym pustym memo"() {
        when:
        def request = TransferRequest.builder()
                .from("PLN-1")
                .to("PLN-2")
                .amount(new BigDecimal("10.00"))
                .build()

        then:
        request.memo() == ""
        request.amount() == new BigDecimal("10.00")
    }

    def "builder waliduje przez konstruktor rekordu"() {
        when:
        TransferRequest.builder()
                .from("PLN-1")
                .to(to)
                .amount(amount)
                .build()

        then:
        thrown(expected)

        where:
        to       | amount                  || expected
        "PLN-1"  | new BigDecimal("1.00")  || IllegalArgumentException
        "PLN-2"  | BigDecimal.ZERO         || IllegalArgumentException
        "PLN-2"  | null                    || NullPointerException
    }
}