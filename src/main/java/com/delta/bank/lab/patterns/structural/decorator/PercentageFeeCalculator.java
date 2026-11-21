package com.delta.bank.lab.patterns.structural.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class PercentageFeeCalculator implements FeeCalculator {

    private final BigDecimal rate;

    public PercentageFeeCalculator(BigDecimal rate) {
        this.rate = Objects.requireNonNull(rate, "rate");
        if (rate.signum() < 0) {
            throw new IllegalArgumentException("rate must not be negative");
        }
    }

    @Override
    public BigDecimal calculate(BigDecimal amount) {
        // FeeCalculator feeCalculatorLambda = a -> a.multiply(new BigDecimal("0.02"));
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}