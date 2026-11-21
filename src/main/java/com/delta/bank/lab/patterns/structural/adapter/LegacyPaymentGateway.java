package com.delta.bank.lab.patterns.structural.adapter;


/*
Legacy oznacza tu starszy, już istniejący system lub interfejs, który nadal jest używany.
LegacyPaymentGateway to więc stary interfejs bramki płatniczej,
do którego nowy kod może się dostosować za pomocą adaptera.
* */

public interface LegacyPaymentGateway {
    void send(String from, String to, long amountInCents);
}