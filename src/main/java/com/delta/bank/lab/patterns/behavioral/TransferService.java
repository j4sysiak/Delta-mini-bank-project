package com.delta.bank.lab.patterns.behavioral;

import java.math.BigDecimal;
import java.util.Objects;

public class TransferService {

    private final FeeStrategy feeStrategy;

    public TransferService(FeeStrategy feeStrategy) {
        this.feeStrategy = Objects.requireNonNull(feeStrategy);
    }

    public BigDecimal totalCost(BigDecimal amount) {
        return amount.add(feeStrategy.feeFor(amount));
    }
}