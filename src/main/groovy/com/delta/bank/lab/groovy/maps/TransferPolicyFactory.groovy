package com.delta.bank.lab.groovy.maps

class TransferPolicyFactory {

    private static final Set<String> ALLOWED_KEYS = [
            "accountNumber",
            "currency",
            "dailyLimit",
            "enabled"
    ] as Set<String>

    private static final BigDecimal DEFAULT_DAILY_LIMIT =
            new BigDecimal("1000.00")

    TransferPolicy create(Map options) {
        Objects.requireNonNull(options, "options must not be null")

        def unknownKeys = options.keySet()
                .findAll { key -> !(key instanceof String) || !ALLOWED_KEYS.contains(key) }

        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown option(s): ${unknownKeys.collect { it.toString() }.sort().join(', ')}"
            )
        }

        if (!options.containsKey("accountNumber")) {
            throw new IllegalArgumentException("accountNumber is required")
        }
        if (!options.containsKey("currency")) {
            throw new IllegalArgumentException("currency is required")
        }

        def accountNumberValue = options["accountNumber"]
        if (!(accountNumberValue instanceof String)) {
            throw new IllegalArgumentException("accountNumber must be a String")
        }
        String accountNumber = accountNumberValue
        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank")
        }

        def currencyValue = options["currency"]
        if (!(currencyValue instanceof String)) {
            throw new IllegalArgumentException("currency must be a String")
        }
        String currency = currencyValue
        if (!(currency ==~ /^[A-Z]{3}$/)) {
            throw new IllegalArgumentException(
                    "currency must be a 3-letter uppercase code"
            )
        }

        BigDecimal dailyLimit
        if (!options.containsKey("dailyLimit")) {
            dailyLimit = DEFAULT_DAILY_LIMIT
        } else {
            def limitValue = options["dailyLimit"]
            if (!(limitValue instanceof BigDecimal)) {
                throw new IllegalArgumentException(
                        "dailyLimit must be a BigDecimal"
                )
            }
            dailyLimit = limitValue
        }
        if (dailyLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "dailyLimit must not be negative"
            )
        }

        boolean enabled = true
        if (options.containsKey("enabled")) {
            def enabledValue = options["enabled"]
            if (!(enabledValue instanceof Boolean)) {
                throw new IllegalArgumentException("enabled must be a Boolean")
            }
            enabled = enabledValue
        }

        return new TransferPolicy(
                accountNumber,
                currency,
                dailyLimit,
                enabled
        )
    }
}