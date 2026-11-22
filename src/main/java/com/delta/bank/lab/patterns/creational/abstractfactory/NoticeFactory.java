package com.delta.bank.lab.patterns.creational.abstractfactory;

// NoticeHeader i NoticeBody to dwa rodzaje produktów.
// NoticeFactory deklaruje tworzenie obu, nie wskazując konkretnego formatu.

public interface NoticeFactory {
    NoticeHeader createHeader();
    NoticeBody createBody();
}