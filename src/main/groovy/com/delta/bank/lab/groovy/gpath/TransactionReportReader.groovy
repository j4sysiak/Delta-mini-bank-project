package com.delta.bank.lab.groovy.gpath

import groovy.json.JsonSlurper

class TransactionReportReader {

    Map read(String json) {
        Objects.requireNonNull(json, "json must not be null")

        def parsed = new JsonSlurper().parseText(json)
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("JSON root must be an object")
        }

        return (Map) parsed
    }

    String accountNumber(Map document) {
        Objects.requireNonNull(document, "document must not be null")

        def account = document["account"]
        if (!(account instanceof Map)) {
            throw new IllegalArgumentException("account must be an object")
        }

        def number = account["number"]
        if (!(number instanceof String) || number.isBlank()) {
            throw new IllegalArgumentException(
                    "account.number must be a non-blank String"
            )
        }

        return number
    }

    List<String> transactionTypes(Map document) {
        List<Map> transactions = transactions(document)

        /*
        { Object type -> ... } to Closure przekazany do collect.
        collect wywołuje go dla każdego elementu transactions.type;
        type jest argumentem closure.
        Samo transactions.type pobiera wartości type z transakcji - myTransactionsTypes.
        * */

        return transactions.type.collect { Object type ->
            if (!(type instanceof String)) {
                throw new IllegalArgumentException(
                        "transaction.type must be a String"
                )
            }
            return (String) type
        }
    }

    List<BigDecimal> depositAmounts(Map document) {
        List<Map> deposits = transactions(document).findAll { Map tx ->
            tx["type"] == "DEPOSIT"
        }

        return deposits.collect { Map tx ->
            Object value = tx["amount"]
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException(
                        "transaction.amount must be a JSON number"
                )
            }
            return new BigDecimal(value.toString())
        }
    }

    private static List<Map> transactions(Map document) {
        Objects.requireNonNull(document, "document must not be null")

        def value = document["transactions"]
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("transactions must be an array")
        }

        List<Map> result = []
        for (Object element : (List) value) {
            if (!(element instanceof Map)) {
                throw new IllegalArgumentException(
                        "each transaction must be an object"
                )
            }
            result.add((Map) element)
        }

        return result
    }
}