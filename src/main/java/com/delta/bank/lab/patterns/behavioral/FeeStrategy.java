package com.delta.bank.lab.patterns.behavioral;

import java.math.BigDecimal;
import java.math.RoundingMode;

/*
Ten interfejs jest funkcyjny, bo ma dokładnie jedną metodę abstrakcyjną: `feeFor`.
Dzięki temu strategie można zapisać krótko jako lambdy — np. `amount -> ...` — i przekazać je tam, gdzie oczekiwany jest `FeeStrategy`.
`FLAT`, `PERCENT` i `FREE` to po prostu trzy gotowe implementacje tej metody.

Może być „zwykły” interfejs, ale jeśli nadal ma jedną metodę abstrakcyjną, nic praktycznie nie zyskasz
        — lambdy nadal będą działać.
`@FunctionalInterface` jedynie dokumentuje intencję i powoduje błąd kompilacji, gdy ktoś doda drugą metodę abstrakcyjną.
Jeśli interfejs miałby kilka takich metod, lambd już nie dałoby się użyć.
 */
@FunctionalInterface
public interface FeeStrategy {

    BigDecimal feeFor(BigDecimal amount);

    FeeStrategy FLAT = amount -> new BigDecimal("2.00");
    //FeeStrategy FLAT = new FeeStrategy() {
    //    @Override
    //    public BigDecimal feeFor(BigDecimal amount) {
    //        return new BigDecimal("2.00");
    //    }
    //};

    FeeStrategy PERCENT = amount ->
                 amount.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
    //FeeStrategy PERCENT = new FeeStrategy() {
    //    @Override
    //    public BigDecimal feeFor(BigDecimal amount) {
    //        return amount.multiply(new BigDecimal("0.01"))
    //                .setScale(2, RoundingMode.HALF_UP);
    //    }
    //};

    FeeStrategy FREE = amount -> BigDecimal.ZERO.setScale(2);
    //FeeStrategy FREE = new FeeStrategy() {
    //    @Override
    //    public BigDecimal feeFor(BigDecimal amount) {
    //        return BigDecimal.ZERO.setScale(2);
    //    }
    //};
}