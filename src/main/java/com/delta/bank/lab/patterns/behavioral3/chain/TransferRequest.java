package com.delta.bank.lab.patterns.behavioral3.chain;

import java.math.BigDecimal;
import java.util.Objects;

/*
Dane przelewu
Record pilnuje, by pola nie były null.
Pozostałe reguły umieścimy w osobnych walidatorach:
  - LimitValidator
  - AccountValidator
  - AmountValidator
  - TransferValidator
  - Nie sprawdzamy tu, czy konta istnieją, ani czy nadawca ma wystarczające saldo.
* */

public record TransferRequest(String from, String to, BigDecimal amount) {

    public TransferRequest {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
    }
}