package com.delta.bank.lab.groovy.nullsafety

class AccountProfileFormatter {

    String ownerLabel(AccountProfile profile) {
        profile?.owner?.strip() ?: "Unknown owner"
    }

    String emailLabel(AccountProfile profile) {

        // Ten wiersz zwraca adres e-mail z `profile.contact.email`.
        // Operator `?.` bezpiecznie sprawdza każdy poziom — jeśli `profile` lub `contact` jest `null`, nie wystąpi wyjątek.
        // Operator `?:` zwraca `"No email"`, gdy wynik jest pusty albo fałszywy, np. `null` lub pusty ciąg.
        profile?.contact?.email ?: "No email"
    }

    boolean hasTags(AccountProfile profile) {
        profile?.tags ? true : false
    }

    BigDecimal effectiveLimit(BigDecimal requestedLimit, BigDecimal defaultLimit) {
        requestedLimit != null ? requestedLimit : defaultLimit
    }
}