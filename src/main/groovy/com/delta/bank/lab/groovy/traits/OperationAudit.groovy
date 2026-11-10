package com.delta.bank.lab.groovy.traits

import java.time.LocalDateTime
import java.time.Clock

// 1. Czym jest trait?
//Trait to element Groovy służący do komponowania zachowania. Może zawierać:
//  - gotowe metody,
//  - metody abstrakcyjne, które klasa implementująca musi dostarczyć,
//  - właściwości i metody pomocnicze.

// Trait - To coś w rodzaju wielokrotnego użycia implementacji:
// klasa może go zaimplementować, dostarczając metody auditClock() i auditSource(), a odziedziczy metodę auditLine(String action).
trait OperationAudit {

    abstract Clock auditClock()  // musi zostać zaimplementowana w klasie implementującej trait

    abstract String auditSource() // musi zostać zaimplementowana w klasie implementującej trait

    // Metoda auditLine() korzysta z metod abstrakcyjnych, które muszą być zaimplementowane w klasie implementującej trait.
    // Trait dostarcza gotową metodę formatującą wpis audytu w formacie: "timestamp|source|action"
    // może być użyty przez różne serwisy bez powielania logiki.
    String auditLine(String action) {
        Objects.requireNonNull(action, "action must not be null")

        if (action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank")
        }

        String source = auditSource()
        if (source == null || source.isBlank()) {
            throw new IllegalStateException("auditSource must not be blank")
        }

        String normalizedAction = action.strip()
        LocalDateTime timestamp = LocalDateTime.now(auditClock())

        return "${timestamp}|${source}|${normalizedAction}"
    }
}