package com.delta.bank.lab.groovy.delegation

class TransferDsl {

    TransferDraft transfer (

            // Adnotacja `@DelegatesTo` opisuje dla Groovy, że wewnątrz tego domknięcia delegatem jest TransferDraftBuilder;
            // metoda ustawia go później przez rehydrate.
            @DelegatesTo (
            // value: skazuje typ obiektu (tu: TransferDraftBuilder), na którym można wywoływać metody wewnątrz domknięcia.
            value = TransferDraftBuilder,  // delegate type
            // strategy = Closure.DELEGATE_ONLY określa, że wywołania w domknięciu mają być rozwiązywane wyłącznie względem delegata
            //    - bez szukania metod u właściciela domknięcia.
            strategy = Closure.DELEGATE_ONLY
            )
            // `Closure<?> specification` to parametr domknięcia Groovy przekazywanego przez wywołującego jako konfiguracja przelewu.
            // ? oznacza, że metoda nie zakłada konkretnego typu wartości zwracanej przez domknięcie.
            Closure<?> specification
    ) {
        Objects.requireNonNull(specification as Object, "specification must not be null")

        def builder = new TransferDraftBuilder()

        Closure<?> configured = specification.rehydrate(  // rehydrate(...) ustawia builder jako delegata
                builder,
                specification.owner,
                specification.thisObject
        )

        // resolveStrategy = Closure.DELEGATE_ONLY sprawia,
        // że wywołania w domknięciu są szukane wyłącznie w TransferDraftBuilder.
        configured.resolveStrategy = Closure.DELEGATE_ONLY

        configured.call()

        return builder.build()  // wywołanie build() na builderze tworzy TransferDraft z ustawionymi właściwościami
    }
}