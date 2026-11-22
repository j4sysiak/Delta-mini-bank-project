package com.delta.bank.lab.patterns.creational.abstractfactory;

public final class TextNoticeFactory implements NoticeFactory {

    @Override
    public NoticeHeader createHeader() {
        return () -> "TRANSFER";
    }

    @Override
    public NoticeBody createBody() {
        return notice -> String
                .format("To: %s, amount: %s PLN", notice.recipient(), notice.amount().toPlainString());
    }
}