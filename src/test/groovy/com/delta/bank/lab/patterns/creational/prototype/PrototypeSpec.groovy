package com.delta.bank.lab.patterns.creational.prototype

import spock.lang.Specification
import spock.lang.Unroll

class PrototypeSpec extends Specification {

    def "kopiowanie zachowuje wartości, ale tworzy odrębny obiekt"() {
        given:
        def original = new TransferDraft(
                "PLN-1", "PLN-2", new BigDecimal("25.00"), ["rent", "monthly"])

        when:
        // Ta linia tworzy kopię original przez metodę copy().
        def copy = original.copy()

        then:
        // test sprawdza, czy kopia jest osobnym obiektem i zachowuje te same wartości;
        //  niezależność listy tagów weryfikuje osobny test.
        !copy.is(original)
        copy.from() == original.from()  // immutable, więc można porównać referencje
        copy.to() == original.to()      // immutable, więc można porównać referencje
        copy.amount().compareTo(original.amount()) == 0  // immutable, więc można porównać referencje
        copy.tags() == original.tags()  // mutable, więc porównujemy zawartość, kopiowanie tworzy niezależną mutowalną listę tagów
    }

    def "kopiowanie tworzy niezależną mutowalną listę tagów"() {
        given:
        def original = new TransferDraft("PLN-1", "PLN-2", BigDecimal.TEN, ["rent"])

        when:
        def copy = original.copy()
        copy.addTag("urgent")

        then:
        copy.tags() == ["rent", "urgent"]
        original.tags() == ["rent"]
    }

    def "konstruktor nie zachowuje mutowalnej listy przekazanej przez wywołującego"() {
        given:
        def inputTags = ["rent"]

        when:
        def draft = new TransferDraft("PLN-1", "PLN-2", BigDecimal.TEN, inputTags)
        inputTags << "external-change"

        then:
        draft.tags() == ["rent"]
    }

    def "accessor tags nie ujawnia wewnętrznej mutowalnej listy"() {
        given:
        def draft = new TransferDraft("PLN-1", "PLN-2", BigDecimal.TEN, ["rent"])

        when:
        // próbujemy zmodyfikować listę zwróconą przez accessor;
        // powinna ona być niezmienialna i rzucać UnsupportedOperationException.
        draft.tags().add("external-change")

        then:
        thrown(UnsupportedOperationException)
        draft.tags() == ["rent"]
    }

    @Unroll
    def "konstruktor odrzuca nieprawidłowy draft: #description"() {
        when:
        new TransferDraft(from, to, amount, tags)

        then:
        thrown(expected)

        where:
        description       | from    | to      | amount                  | tags || expected
        "blank source"    | " "     | "PLN-2" | BigDecimal.ONE          | []   || IllegalArgumentException
        "same accounts"   | "PLN-1" | "PLN-1" | BigDecimal.ONE          | []   || IllegalArgumentException
        "zero amount"     | "PLN-1" | "PLN-2" | BigDecimal.ZERO         | []   || IllegalArgumentException
        "negative amount" | "PLN-1" | "PLN-2" | new BigDecimal("-1.00") | []   || IllegalArgumentException
        "null tags"       | "PLN-1" | "PLN-2" | BigDecimal.ONE          | null || NullPointerException
    }
}