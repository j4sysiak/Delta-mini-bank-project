package com.delta.bank.lab.patterns.structural.facade;

import java.math.BigDecimal;

public record TransferReceipt(
        String from,
        String to,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal totalDebit) {}