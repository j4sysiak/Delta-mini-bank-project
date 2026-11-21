package com.delta.bank.lab.patterns.structural.facade;

import com.delta.bank.lab.patterns.structural.adapter.PaymentGateway;
import com.delta.bank.lab.patterns.structural.decorator.FeeCalculator;

import java.math.BigDecimal;
import java.util.Objects;

public final class TransferFacade {

    private final PaymentGateway gateway;
    private final FeeCalculator feeCalculator;

    public TransferFacade(PaymentGateway gateway, FeeCalculator feeCalculator) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.feeCalculator = Objects.requireNonNull(feeCalculator, "feeCalculator");
    }

    public TransferReceipt transfer(String from, String to, BigDecimal amount) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (from.equals(to)) {
            throw new IllegalArgumentException("accounts must be different");
        }

        BigDecimal fee = feeCalculator.calculate(amount);

        // gateway w teście jest zamockowany,
        // więc nie wykonuje prawdziwego przelewu gateway.transfer(...) — pozwala tylko sprawdzić, czy fasada wywołała transfer(...).
        // przechwytuje wywołanie i pozwala testowi sprawdzić np. argumenty oraz to, czy metoda została wywołana.
        // Jeśli gateway byłby prawdziwą instancją, ta linia fizycznie uruchomiłaby transfer.
        gateway.transfer(from, to, amount);

        //  TransferReceipt to obiekt opisujący wynik przelewu — tutaj zawiera dane nadawcy i odbiorcy, kwotę, opłatę oraz sumę kwoty i opłaty.
        //  Fasada zwraca go po wywołaniu gateway.transfer(...);
        //  sam obiekt potwierdzenia nie wykonuje przelewu.
        return new TransferReceipt(from, to, amount, fee, amount.add(fee));
    }
}