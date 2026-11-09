package com.delta.bank.lab.groovy.delegation

class TransferDsl {

    TransferDraft transfer (

            // Closure przekazane do TransferDsl.transfer(...)

            // Adnotacja `@DelegatesTo` opisuje dla Groovy, że wewnątrz tego domknięcia delegatem jest TransferDraftBuilder;
            // metoda ustawia go później przez rehydrate.
            @DelegatesTo (
            // value: skazuje typ obiektu (tu: TransferDraftBuilder), na którym można wywoływać metody wewnątrz domknięcia.
            value = TransferDraftBuilder,  // delegate type: TransferDraftBuilder zostaje ustawiony jako delegate kopii closure
            // strategy = Closure.DELEGATE_ONLY określa, że wywołania w domknięciu mają być rozwiązywane wyłącznie względem delegata
            //    - bez szukania metod u właściciela domknięcia.
            strategy = Closure.DELEGATE_ONLY
            )
            // `Closure<?> specification` to parametr domknięcia Groovy przekazywanego przez wywołującego jako konfiguracja przelewu.
            // ? oznacza, że metoda nie zakłada konkretnego typu wartości zwracanej przez domknięcie.
            Closure<?> specification
    ) {
        Objects.requireNonNull(specification as Object, "specification must not be null")

        // TransferDraftBuilder zostaje ustawiony jako delegate kopii closure
        // a to jest kluczowe, bo pozwala na wywoływanie metod i ustawianie właściwości TransferDraftBuilder wewnątrz domknięcia.
        def builder = new TransferDraftBuilder()

        // Tworzy kopię domknięcia `specification`, ustawiając dla niej nowy kontekst.
        // Pierwszy argument (`builder`) staje się delegatem, a `owner` i `thisObject`
        // pozostają takie jak w oryginalnym domknięciu.
        Closure<?> configured = specification.rehydrate(
                builder,    // result.delegate = delegate;
                specification.owner,
                specification.thisObject
        )

        // resolveStrategy = Closure.DELEGATE_ONLY sprawia,
        // że wywołania w domknięciu są szukane wyłącznie w TransferDraftBuilder.
        configured.resolveStrategy = Closure.DELEGATE_ONLY

        // configured.call() wypełnia pola buildera na podstawie wywołań w domknięciu przekazanym przez użytkownika.
        configured.call()

        // builder.build() waliduje dane i tworzy TransferDraft
        return builder.build()  // wywołanie build() na builderze tworzy TransferDraft z ustawionymi właściwościami
    }
}