package com.delta.bank.lab.groovy.json

import groovy.json.JsonSlurper

class TransferCommandParser {

    private static final Set<String> ALLOWED_KEYS = [
            "requestId",
            "fromAccount",
            "toAccount",
            "amount"
    ] as Set<String>

    TransferCommand parse(String json) {
        Objects.requireNonNull(json, "json must not be null")

        def parsed = new JsonSlurper().parseText(json)
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("JSON root must be an object")
        }

        Map document = (Map) parsed
        def unknownKeys = document.keySet()
                .findAll { key -> !(key instanceof String) || !ALLOWED_KEYS.contains(key) }

        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown field(s): ${unknownKeys.collect { it.toString() }.sort().join(', ')}"
            )
        }

        String requestId = requiredText(document, "requestId")
        String fromAccount = requiredText(document, "fromAccount")
        String toAccount = requiredText(document, "toAccount")
        BigDecimal amount = requiredAmount(document, "amount")

        if (fromAccount == toAccount) {
            throw new IllegalArgumentException(
                    "fromAccount and toAccount must be different"
            )
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero")
        }

        return new TransferCommand(
                requestId,
                fromAccount,
                toAccount,
                amount
        )
    }

    private static String requiredText(Map document, String field) {
        if (!document.containsKey(field)) {
            throw new IllegalArgumentException("${field} is required")
        }

        def value = document[field]
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("${field} must be a String")
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("${field} must not be blank")
        }

        return value
    }

    private static BigDecimal requiredAmount(Map document, String field) {
        if (!document.containsKey(field)) {
            throw new IllegalArgumentException("${field} is required")
        }

        def value = document[field]
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("${field} must be a JSON number")
        }

        try {
            return new BigDecimal(value.toString())
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "${field} must be a valid decimal number",
                    exception
            )
        }
    }
}