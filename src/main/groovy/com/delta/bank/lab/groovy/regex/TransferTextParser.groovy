package com.delta.bank.lab.groovy.regex

class TransferTextParser {

    private static final String ACCOUNT_NUMBER = /[A-Z]{3}-\d{4}/

    private static final String TRANSFER_PATTERN =
            /TRANSFER\s+(?<from>${ACCOUNT_NUMBER})\s*->\s*(?<to>${ACCOUNT_NUMBER})\s*;\s*amount=(?<amount>\d+(?:\.\d{1,2})?)(?![\d.])/

    boolean isAccountNumber(String value) {
        return value != null && value ==~ /[A-Z]{3}-\d{4}/
    }

    List<String> findAccountNumbers(String text) {
        Objects.requireNonNull(text, "text must not be null")

        def matcher = text =~ /(?<![A-Z0-9-])[A-Z]{3}-\d{4}(?![A-Z0-9-])/
        List<String> accountNumbers = []

        while (matcher.find()) {
            accountNumbers.add(matcher.group())
        }

        return List.copyOf(accountNumbers)
    }

    List<TransferRoute> parseTransfers(String text) {
        Objects.requireNonNull(text, "text must not be null")

        def matcher = text =~ TRANSFER_PATTERN
        List<TransferRoute> transfers = []

        while (matcher.find()) {
            transfers.add(new TransferRoute(
                    matcher.group("from"),
                    matcher.group("to"),
                    new BigDecimal(matcher.group("amount"))
            ))
        }

        return List.copyOf(transfers)
    }
}