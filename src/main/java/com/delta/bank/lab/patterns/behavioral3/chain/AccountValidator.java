package com.delta.bank.lab.patterns.behavioral3.chain;

import java.util.Objects;

public final class AccountValidator implements TransferValidator {

    @Override
    public void validate(TransferRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.from().isBlank() || request.to().isBlank()) {
            throw new IllegalArgumentException("account must not be blank");
        }
        if (request.from().equals(request.to())) {
            throw new IllegalArgumentException("accounts must be different");
        }
    }
}