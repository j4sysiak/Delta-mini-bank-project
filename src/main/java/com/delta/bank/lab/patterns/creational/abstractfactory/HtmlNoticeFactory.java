package com.delta.bank.lab.patterns.creational.abstractfactory;

public final class HtmlNoticeFactory implements NoticeFactory {

    @Override
    public NoticeHeader createHeader() {
        return () -> "<h1>Transfer</h1>";
    }

    @Override
    public NoticeBody createBody() {
        return notice -> String
          .format("<p>To: %s, amount: %s PLN</p>", escapeHtml(notice.recipient()), notice.amount().toPlainString());
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}