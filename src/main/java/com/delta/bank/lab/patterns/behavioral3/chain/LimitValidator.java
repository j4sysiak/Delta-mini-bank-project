package com.delta.bank.lab.patterns.behavioral3.chain;

import java.math.BigDecimal;
import java.util.Objects;

public final class LimitValidator implements TransferValidator {

    private final BigDecimal limit;

    public LimitValidator(BigDecimal limit) {
        this.limit = Objects.requireNonNull(limit, "limit");
        if (limit.signum() <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }

    @Override
    public void validate(TransferRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.amount().compareTo(limit) > 0) {
            throw new IllegalArgumentException("transfer exceeds limit");
        }
    }
}