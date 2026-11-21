package com.delta.bank.lab.patterns.structural.adapter;

import java.math.BigDecimal;
import java.util.Objects;

/*
LegacyPaymentGatewayAdapter jest adapterem:
  implementuje interfejs PaymentGateway, a wywołanie transfer tłumaczy na legacy.send(...), przeliczając kwotę na centy.
  Konstruktor przyjmuje obiekt legacy, który adapter opakowuje.
* */

public final class LegacyPaymentGatewayAdapter implements PaymentGateway {

    private final LegacyPaymentGateway legacy;

    public LegacyPaymentGatewayAdapter(LegacyPaymentGateway legacy) {
        this.legacy = Objects.requireNonNull(legacy, "legacy");
    }

    @Override
    public void transfer(String from, String to, BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        long cents = amount.movePointRight(2).longValueExact();

        // LegacyPaymentGateway oczekuje kwoty wyrażonej w centach.
        // to jest przykład tłumaczenia interfejsu, które adapter wykonuje.
        legacy.send(from, to, cents);
    }
}