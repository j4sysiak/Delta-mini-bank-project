package com.delta.bank.lab.patterns.structural.decorator;

import java.math.BigDecimal;


/*
`FeeCalculator` to interfejs funkcyjny opisujący sposób obliczania opłaty.
Jego jedyna metoda przyjmuje kwotę (`amount`) i zwraca wyliczoną opłatę jako `BigDecimal`.

Adnotacja `@FunctionalInterface` oznacza, że interfejs ma jedną metodę abstrakcyjną,
więc można go implementować lambdą, np. `amount -> amount.multiply(new BigDecimal("0.02"))` dla opłaty 2%.
Wzorzec dekoratora może używać go do dodawania lub zmieniania naliczania opłat.
* */
@FunctionalInterface
public interface FeeCalculator {
    BigDecimal calculate(BigDecimal amount);
}