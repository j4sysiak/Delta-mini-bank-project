package com.delta.bank.lab.patterns.behavioral1.strategy

import com.delta.bank.lab.patterns.behavioral1.FeeStrategy
import com.delta.bank.lab.patterns.behavioral1.TransferService
import spock.lang.Specification

/*
 To jest test wzorca **Strategy (Strategia)**.
 `TransferService` wykonuje tę samą operację — oblicza koszt przelewu
 — ale sposób naliczania opłaty przekazuje do osobnego obiektu, `strategy`.

   Tabela `where` sprawdza kilka strategii dla kwoty `100.00`:
   `FLAT` daje koszt `102.00` (opłata 2),
   `PERCENT` — `101.00` (1%),
   `FREE` — `100.00` (bez opłaty).

   Dzięki temu regułę opłaty można zmieniać bez zmieniania samego serwisu.
   To test parametryzowany Spocka: uruchamia tę samą asercję dla każdego wiersza tabeli.

  */

class StrategyPatternsSpec extends Specification {


    /*
    To jest test wzorca **Strategy (Strategia)**.
    `TransferService` wykonuje tę samą operację — oblicza koszt przelewu
    — ale sposób naliczania opłaty przekazuje do osobnego obiektu, `strategy`: `feeStrategy.feeFor(amount)`

        return amount.add(feeStrategy.feeFor(amount));

      Tabela `where` sprawdza kilka strategii dla kwoty `100.00`:
      `FLAT` daje koszt `102.00` (opłata 2),
      `PERCENT` — `101.00` (1%),
      `FREE` — `100.00` (bez opłaty).

      Dzięki temu regułę opłaty można zmieniać bez zmieniania samego serwisu.
      To test parametryzowany Spocka: uruchamia tę samą asercję dla każdego wiersza tabeli.
     */
    def "strategy changes the fee without changing the service: TransferService"() {
        expect:
        new TransferService(strategy)
                .totalCost(new BigDecimal("100.00")) == total

        where:
        strategy              || total
        FeeStrategy.FLAT      || new BigDecimal("102.00")
        FeeStrategy.PERCENT   || new BigDecimal("101.00")
        FeeStrategy.FREE      || new BigDecimal("100.00")
    }

    def "strategy can be a lambda"() {
        given:
        FeeStrategy custom = amount -> new BigDecimal("5.00")  // strategy

        expect:
        new TransferService(custom)
                .totalCost(new BigDecimal("10.00"))
                                           == new BigDecimal("15.00")
    }

    def "service rejects null strategy"() {
        when:
        new TransferService(null)

        then:
        thrown(NullPointerException)
    }


}