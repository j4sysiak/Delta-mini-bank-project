package com.delta.bank.lab.groovy.json

import groovy.json.JsonOutput

class TransferCommandJson {

    String write(TransferCommand command) {
        if (command == null) {
            throw new NullPointerException("command must not be null")
        }

        Map document = [
                requestId : command.requestId,
                fromAccount: command.fromAccount,
                toAccount  : command.toAccount,
                amount     : command.amount
        ]

        return JsonOutput.toJson(document)
    }
}