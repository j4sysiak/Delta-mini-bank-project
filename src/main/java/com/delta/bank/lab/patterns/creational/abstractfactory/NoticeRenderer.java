package com.delta.bank.lab.patterns.creational.abstractfactory;

import java.util.Objects;

// Klient wie, że potrzebuje nagłówka i treści, ale nie zna konkretnych implementacji.
// Fabrykę wybieramy na zewnątrz.
// Przykład w testach AbstractFactorySpec:
//          NoticeRenderer renderer = new NoticeRenderer(new TextNoticeFactory());
//          String message = renderer.render(new TransferNotice("Anna", new BigDecimal("25.00")));

public final class NoticeRenderer {

    private final NoticeFactory factory;

    public NoticeRenderer(NoticeFactory factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    public String render(TransferNotice notice) {
        Objects.requireNonNull(notice, "notice");
        NoticeHeader header = factory.createHeader();
        NoticeBody body = factory.createBody();
        return header.render() + "\n" + body.render(notice);
    }
}