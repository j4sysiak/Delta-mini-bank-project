package com.delta.bank.lab.patterns.structural.decorator;

import java.math.BigDecimal;
import java.util.Objects;

public final class MinimumFeeDecorator implements FeeCalculator {

    /*
    Obiekt `MinimumFeeDecorator` oblicza podstawowej opłaty:
     - wywołuje `calculator.calculate(amount)` i wykonuje obliczenia w PercentageFeeCalculator
     - a potem nakłada własną regułę minimalnej opłaty.

    Czyli `delegate` wykonuje bazową pracę, a dekorator ją uzupełnia.
    Nazwa opisuje jego rolę — to nie specjalny typ Javy, tylko zwyczajowa nazwa pola.
    * */


    private final FeeCalculator calculator;
    private final BigDecimal minimum;

    public MinimumFeeDecorator(FeeCalculator calculator, BigDecimal minimum) {
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.minimum = Objects.requireNonNull(minimum, "minimum");
        if (minimum.signum() < 0) {
            throw new IllegalArgumentException("minimum must not be negative");
        }
    }

    // Wzorzec dekoratora polega na tym, że implementujemy ten sam interfejs co dekorowany obiekt (u nas FeeCalculator),
    // a w metodzie wywołujemy metodę dekorowanego obiektu (u nas calculator.calculate(amount)) i ewentualnie modyfikujemy wynik.
    @Override
    public BigDecimal calculate(BigDecimal amount) {
        BigDecimal calculatedAmount = calculator.calculate(amount);
        return calculatedAmount.compareTo(minimum) < 0 ? minimum : calculatedAmount;
    }

    /*
    public interface FeeCalculator {
       BigDecimal calculate(BigDecimal amount);
    }

    FeeCalculator feeCalculator = new FeeCalculator() {
        @Override
        public BigDecimal calculate(BigDecimal amount) {
            return amount.multiply(new BigDecimal("0.02"));
        }
    };

    FeeCalculator feeCalculatorLambda = a -> a.multiply(new BigDecimal("0.02"));

    * */
}