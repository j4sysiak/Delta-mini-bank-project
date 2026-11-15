package com.delta.bank.lab.patterns.behavioral2.command

import spock.lang.Specification

class CommandPatternsSpec extends Specification {

    def "command is deferred until the invoker runs it"() {
        given:
        def account = new BankAccount()
        def queue = new CommandQueue()
        queue.submit(new DepositCommand(account, new BigDecimal("25.00")))

        expect:
        account.balance() == BigDecimal.ZERO
        queue.size() == 1

        when:
        queue.runNext()

        then:
        account.balance() == new BigDecimal("25.00")
        queue.size() == 0
    }

    def "queue runs commands in submission order"() {
        given:
        def account = new BankAccount()
        def queue = new CommandQueue()
        queue.submit(new DepositCommand(account, new BigDecimal("10.00")))
        queue.submit(new DepositCommand(account, new BigDecimal("5.00")))

        when:
        queue.runAll()

        then:
        account.balance() == new BigDecimal("15.00")
        queue.runNext() == false
    }

}