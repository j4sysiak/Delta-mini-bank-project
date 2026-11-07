package com.delta.bank.lab.groovy.closures

class BankTransactionAnalyzer {

    private final List<BankTransaction> transactions

    BankTransactionAnalyzer(List<BankTransaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null")

        //  { tx -> tx == null } to closure w Groovy.
        //  Metoda any przyjmuje ją jako argument i wywołuje dla kolejnych elementów listy.
        //  Parametr tx oznacza sprawdzany element, a closure zwraca true, jeśli jest on równy null.
        if (transactions.any { tx -> tx == null }) {
            throw new IllegalArgumentException("transactions must not contain null")
        }

        // Ta linia tworzy niezmienną kopię listy transactions i zapisuje ją w polu obiektu.
        // Dzięki temu późniejsze zmiany w oryginalnej liście przekazanej do konstruktora nie zmienią listy analizatora.
        // Elementy listy — obiekty BankTransaction — nie są jednak kopiowane,
        //   a List.copyOf zgłosi wyjątek, jeśli lista zawiera null.
        this.transactions = List.copyOf(transactions)
    }

    List<BankTransaction> findByKind(TransactionKind kind) {
        Objects.requireNonNull(kind, "kind must not be null")

         //  { tx -> tx.kind == kind } to closure w Groovy — funkcja przekazana jako argument do findAll
        transactions.findAll { tx -> tx.kind == kind }
    }

    List<String> descriptions() {
        transactions.collect { tx ->
            "${tx.accountNumber}: ${tx.kind} ${tx.amount.toPlainString()}"
        }
        //  { tx -> ... } to closure w Groovy, czyli blok kodu, który można przekazać jako argument.
        //  Tutaj collect uruchamia ją dla każdej transakcji z listy.
        //  tx oznacza bieżącą transakcję, a tekst w cudzysłowie jest wynikiem closure.
        //    tzn: closure tworzy dla niej tekst, np.: "123: DEPOSIT 50.00" lub "123: WITHDRAWAL 25.00".
        //  collect zbiera te wyniki do nowej listy String, np. ["123: DEPOSIT 50.00", ...].
    }

    BankTransaction firstAtLeast(TransactionKind kind, BigDecimal minimum) {
        Objects.requireNonNull(kind, "kind must not be null")
        Objects.requireNonNull(minimum, "minimum must not be null")

        transactions.find { tx ->
            tx.kind == kind && tx.amount.compareTo(minimum) >= 0
        }
    }

    Map<TransactionKind, List<BankTransaction>> groupByKind() {
        transactions.groupBy { tx ->
            tx.kind
        }
    }

    BigDecimal totalFor(TransactionKind kind) {
        Objects.requireNonNull(kind, "kind must not be null")

        transactions
                .findAll { tx -> tx.kind == kind }
                // Następnie inject zaczyna od BigDecimal.ZERO i dla każdej z nich wykonuje closure:
                //  closure przyjmuje dwa parametry: total i tx.
                //  total to dotychczasowa suma, a tx to bieżąca transakcja.
                //  Closure zwraca nową sumę, czyli total + tx.amount.
                // Wynik całego wyrażenia to suma kwot; jeśli nie ma pasujących transakcji, wynosi BigDecimal.ZERO.
                .inject(BigDecimal.ZERO) { BigDecimal total,  // zaczynamy od sumy równej zero
                                           BankTransaction tx -> total.add(tx.amount)
                }
    }
}