package com.delta.bank.lab.patterns.behavioral3.chain;

import java.math.BigDecimal;

//  Złożenie łańcucha walidatorów w jeden obiekt TransferValidator
// W tym wariancie każde ogniwo, które nie zgłosi błędu, przekazuje żądanie dalej;
// wyjątek przerywa cały łańcuch.
// W klasycznym ujęciu handler może także sam zdecydować, czy obsłuży żądanie i zakończy przetwarzanie, czy przekaże je następnemu.
// Ta wersja jest łańcuchem walidacji, a nie przykładem wyboru jednego handlera.


// Klasa składa i zwraca łańcuch walidatorów
final class TransferChainValidatorFactory {

    private TransferChainValidatorFactory() {}

    static TransferValidator standard(BigDecimal limit) {

        /*
        samo andThen tylko tworzy i zwraca lambdę — nie uruchamia jeszcze walidacji.
          Przyklad:
             TransferValidator chain = walidatorA.andThen(walidatorB); // tylko tworzy łańcuch
             chain.validate(request); // uruchamia A, a potem B
        * */

        TransferValidator chain = new AccountValidator()
                .andThen(new AmountValidator())
                .andThen(new LimitValidator(limit));

        return chain;
  }
}