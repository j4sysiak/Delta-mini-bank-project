package com.delta.bank.lab.patterns.behavioral3.chain;

import java.util.Objects;

public final class AmountValidator implements TransferValidator {

    @Override
    public void validate(TransferRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}