package com.delta.bank.lab.patterns.behavioral3.chain

import spock.lang.Specification
import spock.lang.Unroll

class ChainOfResponsibilitySpec extends Specification {

    def "poprawny przelew przechodzi przez cały łańcuch, także na granicy limitu"() {
        given:
        def transferChainValidator = TransferChainValidatorFactory.standard(new BigDecimal("100.00"))
        def transferRequest = new TransferRequest("PLN-1", "PLN-2", new BigDecimal("100.0"))

        when:
        // to uruchomienie łańcucha walidatorów,
        // które w tym wariancie nie zwracają żadnej wartości, a jedynie zgłaszają wyjątek w przypadku błędu
        transferChainValidator.validate(transferRequest)

        then:
        noExceptionThrown()
    }

    @Unroll
    def "niepoprawne żądanie jest odrzucane: #reason"() {
        given:
        def transferChainValidator = TransferChainValidatorFactory.standard(new BigDecimal("100.00"))

        when:
        transferChainValidator.validate(new TransferRequest(from, to, new BigDecimal(amount)))

        then:
        def error = thrown(IllegalArgumentException)
        error.message == reason

        where:
        from    | to      | amount   || reason
        " "     | "PLN-2" | "1.00"   || "account must not be blank"
        "PLN-1" | "PLN-1" | "1.00"   || "accounts must be different"
        "PLN-1" | "PLN-2" | "0.00"   || "amount must be positive"
        "PLN-1" | "PLN-2" | "-1.00"  || "amount must be positive"
        "PLN-1" | "PLN-2" | "100.01" || "transfer exceeds limit"
    }

    def "walidatory działają w skonfigurowanej kolejności"() {
        given:
        def calls = []

        // Ten wiersz tworzy pierwszy walidator jako Closure,
        // który dodaje tekst "first" do listy calls[] przy wywołaniu `validate(request)` w części when: `chain.validate(request)`.
        // Podczas budowy łańcucha walidatory są połączone metodą andThen; chain.validate(request) uruchamia kolejno closure’y first, second i third.
        // Ten `Closure` jest zgodny z interfejsem TransferValidator, więc może być użyty jako walidator w łańcuchu.
        // Kolejność wywołań walidatorów jest testowana poprzez sprawdzenie zawartości listy calls[] po uruchomieniu całego łańcucha.
        //
        // Gdy zostanie wywołane `firstValidator.validate(request)`, Closure dodaje tekst "first" do listy calls.
        // To właśnie pozwala testować kolejność wykonania kolejnych walidatorów.
        TransferValidator firstValidator = { request -> calls << "first" }   // calls[] = ["first"]   (dodaje "first" do listy calls)

        TransferValidator secondValidator = { request -> calls << "second" } // calls[] = ["first", "second"]  (dodaje "second" do listy calls)

        TransferValidator thirdValidator = { request -> calls << "third" }   // calls[] = ["first", "second", "third"]  (dodaje "third" do listy calls)

        // tutaj tworzymy łańcuch walidatorów, który uruchomi kolejno firstValidator, secondValidator i thirdValidator
        // Najpierw firstValidator.andThen(secondValidator) tworzy nowy walidator wykonujący kolejno firstValidator,
        // a potem secondValidator.
        // Następnie .andThen(thirdValidator) dodaje thirdValidator na końcu.
        def chain = firstValidator
                .andThen(secondValidator)
                .andThen(thirdValidator)
        // Pierwsze validate(request) uruchomi kolejno: "first", "second", "third"


        def request = new TransferRequest("A", "B", BigDecimal.ONE)

        when:
        // walidujemy `jakiś` request, który przechodzi przez cały łańcuch walidatorów
        // to chain zawiera informacje o kolejności wywołań walidatorów,
        // więc po wywołaniu chain.validate(request) lista calls[] powinna zawierać ["first", "second", "third"]
        chain.validate(request)

        then:
        // po wywołaniu chain.validate(request) lista calls[] powinna zawierać ["first", "second", "third"]
        calls == ["first", "second", "third"]
    }

    def "walidator odrzucający żądanie uniemożliwia uruchomienie kolejnego walidatora"() {
        given:
        def next = Mock(TransferValidator)

        def chain = new AmountValidator()   // tu powinna być podana wartość graniczna, np. 100.00, aby walidator AmountValidator mógł sprawdzić, czy kwota jest większa od zera
                                             .andThen(next)

        def request = new TransferRequest("A", "B", BigDecimal.ZERO)

        when:
        chain.validate(request)

        then:
        thrown(IllegalArgumentException)
        0 * next.validate(_)
    }

    def "null jako kolejny walidator jest odrzucany podczas budowania łańcucha"() {
        when:
        new AccountValidator().andThen(null)

        then:
        thrown(NullPointerException)
    }
}