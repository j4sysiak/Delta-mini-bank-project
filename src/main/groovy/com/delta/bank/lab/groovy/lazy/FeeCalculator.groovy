package com.delta.bank.lab.groovy.lazy

import groovy.transform.Memoized

class FeeCalculator {

    int calls = 0

    /*
    @Memoized (z dwoma „i”) zapamiętuje wynik metody dla przekazanych argumentów.
    Przy kolejnym wywołaniu z tym samym argumentem zwraca zapisany wynik zamiast ponownie wykonywać ciało metody
    — więc calls++ nie zwiększy się.
    Metoda feeFor(...) przechowuje wyniki bez jawnego limitu,
    * */
    @Memoized
    BigDecimal feeFor(BigDecimal amount) {
        calls++
        amount.multiply(new BigDecimal("0.01")).setScale(2, java.math.RoundingMode.HALF_UP)
    }

    // a label ogranicza pamięć podręczną do 2 wpisów przez maxCacheSize = 2.

    /*
    Ten fragment oznacza, że label zapamiętuje wyniki dla maksymalnie dwóch różnych argumentów:

    label("A") // zwraca "FEE-A", calls rośnie o 1
    label("A") // zwraca zapamiętane "FEE-A", calls bez zmian
    label("B") // zwraca "FEE-B", calls rośnie o 1

    Gdy pojawi się trzeci kod, np. label("C"), pamięć podręczna usunie jeden ze starszych wpisów,
    żeby zrobić miejsce.
    Jeśli potem wywołasz metodę z usuniętym kodem, metoda wykona się ponownie i calls wzrośnie.
    calls liczy więc faktyczne wykonania metody, a nie każde jej wywołanie.
    * */

    @Memoized(maxCacheSize = 2)
    String label(String code) {
        calls++
        "FEE-" + code
    }
}