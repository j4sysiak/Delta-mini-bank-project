package com.delta.bank.lab.patterns.creational;

import java.math.BigDecimal;
import java.util.Objects;

public record TransferRequest(String from, String to, BigDecimal amount, String memo) {

    public TransferRequest {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("same account");
        }
        memo = memo == null ? "" : memo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String from;
        private String to;
        private BigDecimal amount;
        private String memo;

        public Builder from(String from) {
            this.from = from; return this;
        }

        public Builder to(String to) {
            this.to = to; return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount; return this;
        }

        public Builder memo(String memo) {
            this.memo = memo; return this;
        }

        public TransferRequest build() {
            return new TransferRequest(from, to, amount, memo);
        }
    }
}