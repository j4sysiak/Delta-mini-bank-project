package com.delta.bank.lab.groovy.traits

import java.time.Clock

// wymaga od tej klasy (BankAuditService), która to implementuje trait: OperationAudit
//         - dostarczenia zegara (clock) i nazwy źródła (source),
// a następnie używa tych informacji do tworzenia wpisów audytu w normalizowanym formacie w klasie implementującej trait - OperationAudit.
// W odróżnieniu od klasy bazowej, klasa może implementować wiele traits.
// Trait nie jest jednak automatycznie lepszy od klasy bazowej ani interfejsu:
//   - wybierz go wtedy, gdy chcesz składać zachowanie niezależnie od hierarchii dziedziczenia.
class BankAuditService implements OperationAudit {

    private final Clock clock
    private final String source

    BankAuditService(Clock clock, String source) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null")
        this.source = Objects.requireNonNull(source, "source must not be null")
    }

    @Override
    Clock auditClock() {
        return clock
    }

    @Override
    String auditSource() {
        return source
    }
}