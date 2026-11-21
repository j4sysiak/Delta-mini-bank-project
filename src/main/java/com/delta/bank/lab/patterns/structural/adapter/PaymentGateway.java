package com.delta.bank.lab.patterns.structural.adapter;

import java.math.BigDecimal;

public interface PaymentGateway {
    void transfer(String from, String to, BigDecimal amount);
}