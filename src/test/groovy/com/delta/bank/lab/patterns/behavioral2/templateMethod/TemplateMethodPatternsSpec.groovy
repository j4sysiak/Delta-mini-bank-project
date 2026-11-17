package com.delta.bank.lab.patterns.behavioral2.templateMethod

import spock.lang.Specification

/*
Wzorzec Template Method (Metoda Szablonowa) - definiuje w klasie bazowej `StatementGenerator` szkielet algorytmu i kolejność jego kroków,
pozostawiając podklasom implementację wybranych etapów.

W tym teście `generate` w generatorze zestawienia wykonuje całą procedurę:
 - zapisuje nagłówek, wiersze transakcji, a na końcu stopkę `END`.
 `CsvStatementGenerator` dostarcza format CSV.
 Test sprawdza też, że przy pustej liście transakcji nagłówek i stopka nadal są generowane.
 */

class TemplateMethodPatternsSpec extends Specification {

    def "metoda szablonowa generuje nagłówek, wiersze i stopkę we właściwej kolejności"() {
        given:
        def generator = new CsvStatementGenerator()
        def transactions = [
                new StatementGenerator.Transaction("salary", new BigDecimal("100.00")),
                new StatementGenerator.Transaction("rent", new BigDecimal("-25.00"))
        ]

        expect:
        generator.generate(transactions) == "description,amount\nsalary,100.00\nrent,-25.00\nEND\n"
    }

    def "metoda szablonowa generuje puste zestawienie"() {
        given:
        def generator = new CsvStatementGenerator()

        expect:
        generator.generate([]) == "description,amount\nEND\n"
    }
}