package com.delta.bank.lab.patterns.behavioral;

import java.math.BigDecimal;

public sealed interface BalanceEvent {

    String accountId();

    // `Deposited` to rekord implementujący interfejs `BalanceEvent`.
    //  Przechowuje `accountId` i `amount`, a Java automatycznie generuje konstruktor oraz metody dostępowe, m.in. `accountId()` wymagane przez interfejs.
    // `Withdrawn` jest drugim dozwolonym typem zdarzenia.
    record Deposited(String accountId, BigDecimal amount) implements BalanceEvent {}

    // `Withdrawn` to rekord implementujący interfejs `BalanceEvent`.
    record Withdrawn(String accountId, BigDecimal amount) implements BalanceEvent {}
}