package com.delta.bank.lab.patterns.creational.abstractfactory;

@FunctionalInterface
public interface NoticeBody {
    String render(TransferNotice notice);
}