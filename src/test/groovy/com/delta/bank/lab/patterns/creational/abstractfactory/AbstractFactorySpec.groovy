package com.delta.bank.lab.patterns.creational.abstractfactory

import spock.lang.Specification
import spock.lang.Unroll

class AbstractFactorySpec extends Specification {

    @Unroll
    def "renderer używa spójnej rodziny produktów w formacie #format: (text, HTML)"() {
        given:
        def renderer = new NoticeRenderer(factory)
        def notice = new TransferNotice("Anna", new BigDecimal("25.00"))

        expect:
        renderer.render(notice) == expected

        where:
        format | factory                 || expected
        "text" | new TextNoticeFactory() || "TRANSFER\nTo: Anna, amount: 25.00 PLN"
        "HTML" | new HtmlNoticeFactory() || "<h1>Transfer</h1>\n<p>To: Anna, amount: 25.00 PLN</p>"
    }

    def "HTML body ucieka tekst odbiorcy"() {
        given:
        def renderer = new NoticeRenderer(new HtmlNoticeFactory())
        def notice = new TransferNotice('<Anna & "Co">', BigDecimal.ONE)

        expect:
        renderer.render(notice) ==
                '<h1>Transfer</h1>\n<p>To: &lt;Anna &amp; &quot;Co&quot;&gt;, amount: 1 PLN</p>'
    }

    def "renderer pyta dostarczoną fabrykę o oba produkty"() {
        given:
        def factory = Mock(NoticeFactory)
        // NoticeHeader header = { () -> "HEADER" }  // lambda, która implementuje metodę render() z NoticeHeader
        NoticeHeader header =  {  -> "HEADER" }

        /*
        NoticeHeader header = new NoticeHeader() {
            @Override
            String render() {
                return null
            }
        };

       NoticeHeader header2 = new NoticeHeader() {
            @Override
            String render() {
                return "HEADER"
            }
        };
        lambda: NoticeHeader header = { () -> "HEADER" }  // to samo co powyżej, ale krócej
        lambda: NoticeHeader header = { -> "HEADER" }     // to samo co powyżej, ale jeszcze krócej

         */

        NoticeBody body = { notice -> "BODY: " + notice.recipient() }
        def renderer = new NoticeRenderer(factory)

        when:
        def result = renderer.render(new TransferNotice("Anna", BigDecimal.ONE))

        then:
        1 * factory.createHeader() >> header  //oczekuję dokładnie jednego wywołania factory.createHeader() i w odpowiedzi zwróć header.
        1 * factory.createBody() >> body
        result == "HEADER\nBODY: Anna"
    }

    def "renderer odrzuca nullową fabrykę"() {
        when:
        new NoticeRenderer(null)

        then:
        thrown(NullPointerException)
    }

    @Unroll
    def "notice odrzuca nieprawidłowe dane: #description"() {
        when:
        new TransferNotice(recipient, amount)

        then:
        thrown(IllegalArgumentException)

        where:
        description       | recipient | amount
        "blank recipient" | "  "      | BigDecimal.ONE
        "zero amount"     | "Anna"    | BigDecimal.ZERO
        "negative amount" | "Anna"    | new BigDecimal("-1.00")
    }
}