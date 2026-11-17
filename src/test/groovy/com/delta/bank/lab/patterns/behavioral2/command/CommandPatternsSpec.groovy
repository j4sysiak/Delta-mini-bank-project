package com.delta.bank.lab.patterns.behavioral2.command

import spock.lang.Specification

class CommandPatternsSpec extends Specification {

    // Wzorzec Command opakowuje żądanie jako obiekt; kolejka (invoker) może je odroczyć.
    // DepositCommand deleguje wykonanie do BankAccount (receiver).

    def "Polecenie zostaje odroczone do momentu, gdy wywołujący je wykona"() {
        given:
        def account = new BankAccount()
        def queue = new CommandQueue()

        // Ta linia tworzy polecenie wpłaty 25,00 na podane konto i dodaje je do kolejki queue.
        // Sama wpłata jeszcze się nie wykonuje — nastąpi dopiero po wywołaniu queue.runNext() albo queue.runAll().
        queue.submit(new DepositCommand(account, new BigDecimal("25.00")))

        expect:
        account.balance() == BigDecimal.ZERO
        queue.size() == 1

        when:
        // Wykonuje następne polecenie z kolejki — wpłata zostaje zaksięgowana, a polecenie usunięte z kolejki.
        queue.runNext()

        then:
        account.balance() == new BigDecimal("25.00")
        queue.size() == 0
    }

    def "Kolejka wykonuje polecenia w kolejności ich zgłoszenia"() {
        given:
        def account = new BankAccount()
        def queue = new CommandQueue()
        queue.submit(new DepositCommand(account, new BigDecimal("10.00")))
        queue.submit(new DepositCommand(account, new BigDecimal("5.00")))

        when:
        // Wykonuje wszystkie polecenia w kolejce — wpłaty zostają zaksięgowane, a polecenia usunięte z kolejki.
        queue.runAll()

        then:
        account.balance() == new BigDecimal("15.00")
        queue.runNext() == false
    }

}