package com.delta.bank.lab.patterns.behavioral3.chain;

import java.util.Objects;

@FunctionalInterface
public interface TransferValidator {

    void validate(TransferRequest request);

    // Ten fragment pozwala łączyć walidatory przelewu w jeden łańcuch.
    //  1. Objects.requireNonNull(next, "next") pilnuje, by drugi walidator nie był null — w przeciwnym razie od razu zgłosi NullPointerException.
    //  2. return request -> { ... } tworzy nowy walidator, który dla tego samego żądania najpierw uruchamia bieżący walidator (validate(request)),
    //     a potem next.
    //  3. Jeśli pierwszy walidator rzuci wyjątek, wykonanie zatrzyma się i drugi nie zostanie wywołany.
    //
    // Przykładowo walidatorA.andThen(walidatorB) oznacza: najpierw sprawdź A, a jeśli przejdzie — sprawdź B.
    // Oryginalne walidatory pozostają bez zmian.

    default TransferValidator andThen(TransferValidator next) {
        Objects.requireNonNull(next, "next");

        // To można nazwać Closure.
        // Zwrócona lambda przechwytuje next oraz bieżące this (czyli walidator, którego andThen wywołano).
        // Gdy później zostanie wywołana, użyje ich, by uruchomić oba walidatory po kolei.
        // Ten fragment zostaje wykonany później, gdy ktoś wywoła metodę validate na walidatorze zwróconym przez andThen.
        // Samo andThen tylko tworzy i zwraca lambdę — nie uruchamia jeszcze walidacji.
        // Przykład:
        //      TransferValidator chain = walidatorA.andThen(walidatorB); // tylko tworzy łańcuch
        //      chain.validate(request); // uruchamia A, a potem B
        return request -> {
            this.validate(request);   // walidator, którego andThen wywołano
            next.validate(request);   // walidator przekazany jako argument jako `next` do andThen
        };
    }
}