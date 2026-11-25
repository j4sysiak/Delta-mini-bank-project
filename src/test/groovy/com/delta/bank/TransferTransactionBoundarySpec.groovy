package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class TransferTransactionBoundarySpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    @Autowired
    PlatformTransactionManager transactionManager

    private static final String FROM = "PLN-4801"
    private static final String TO = "PLN-4802"

    def "udany przelew zatwierdza salda i oba wpisy w historii"() {
        given:
        openAccounts()

        when:
        boolean executed = service.transfer(
                "lab48-success", FROM, TO, new BigDecimal("25.00"))

        then:
        executed
        // Ta asercja sprawdza, czy saldo konta FROM po udanym przelewie wynosi 75,00
        // — początkowe 100,00 minus przelew 25,00. compareTo(...) == 0 porównuje wartość liczbową,
        // więc nie wymaga identycznej skali zapisu (np. 75.0 też przejdzie).

        // fizycznie, w bazie danych, po udanym przelewie, saldo konta FROM powinno wynosić 75.00, a konta TO 45.00 (20.00 + 25.00).
        balanceOf(FROM).compareTo(new BigDecimal("75.00")) == 0
        balanceOf(TO).compareTo(new BigDecimal("45.00")) == 0
        historyFor("lab48-success") == [
                [account: FROM, type: "TRANSFER_OUT", amount: new BigDecimal("25.00")],
                [account: TO, type: "TRANSFER_IN", amount: new BigDecimal("25.00")]
        ]
    }

    // Drugi test wywołuje błąd po wykonaniu logiki przelewu, ale jeszcze przed zatwierdzeniem transakcji otaczającej.
    // To ważniejsze niż wyjątek walidacji rzucony przed pierwszą zmianą salda.
    def "błąd po przelewie wycofuje transakcję nadrzędną"() {
        given:
        openAccounts()

        // To tworzy obiekt Springa `TransactionTemplate`, który pozwala zarządzać transakcją programowo.
        // Otrzymuje `transactionManager`, a poniżej `executeWithoutResult` uruchamia przelew w tej transakcji
        // — jeśli blok rzuci wyjątek, zmiany zostaną wycofane.
        def transaction = new TransactionTemplate(transactionManager)

        when:
        // Kod `{ status -> ... }` to **Closure w Groovy** — blok przekazany jako argument do `executeWithoutResult`.
        // Przyjmuje parametr `status`, ale w tym kodzie nie jest on używany.
        // Spring konwertuje Closure na interfejs funkcyjny wymagany przez tę metodę.
        transaction.executeWithoutResult { status ->
            service.transfer("lab48-rollback", FROM, TO, new BigDecimal("25.00"))
            throw new IllegalStateException("failure after transfer")  // <--- to tu wywołuje rollback całej transakcji tzn po przelewie, ale przed zatwierdzeniem transakcji
        }

        /*

        default void executeWithoutResult(Consumer<TransactionStatus> action) throws TransactionException {
		execute(status -> {
			action.accept(status);
			return null;
		});

		potrzebujemy wiec: `Consumer<TransactionStatus> action`
		-------------------------------------------------------

        @FunctionalInterface
        public interface Consumer<T> {
          void accept(T t);
        }

        Consumer<TransactionStatus> consumer = new Consumer<TransactionStatus>() {
            @Override
            public void accept(TransactionStatus t) {
                // implementacja
            }
        };

        Consumer<TransactionStatus> consumer = (TransactionStatus status) -> {
            // implementacja
            service.transfer("lab48-rollback", FROM, TO, new BigDecimal("25.00"))
            throw new IllegalStateException("failure after transfer")
        };

        Consumer<TransactionStatus> action = status -> {
            // implementacja
            service.transfer("lab48-rollback", FROM, TO, new BigDecimal("25.00"))
            throw new IllegalStateException("failure after transfer")
        };

        transaction.executeWithoutResult(action)

        transaction.executeWithoutResult { status ->
            service.transfer("lab48-rollback", FROM, TO, new BigDecimal("25.00"))
            throw new IllegalStateException("failure after transfer")
        }

        * */

        then:
        def error = thrown(IllegalStateException)
        error.message == "failure after transfer"

        // Po wycofaniu transakcji nadrzędnej, salda kont powinny pozostać niezmienione.
        // fizycznie w bazie danych, po wycofaniu transakcji nadrzędnej, saldo konta FROM powinno wynosić 100.00, a konta TO 20.00 (bez zmian).
        balanceOf(FROM).compareTo(new BigDecimal("100.00")) == 0
        balanceOf(TO).compareTo(new BigDecimal("20.00")) == 0
        historyFor("lab48-rollback").isEmpty()
    }

    def "wyjątek po zakończeniu transakcji nie cofa przelewu"() {
        given:
        openAccounts()

        when:
        service.transfer("lab48-already-committed", FROM, TO, new BigDecimal("25.00"))
        throw new IllegalStateException("failure outside transaction")  // <--- to tu wywołuje wyjątek poza transakcją, tzn po jej zatwierdzeniu

        then:
        thrown(IllegalStateException)

        // Po wywołaniu wyjątku poza transakcją, salda kont powinny pozostać zmienione.
        // fizycznie w bazie danych, po wywołaniu wyjątku poza transakcją,
        // saldo konta FROM powinno wynosić 75.00, a konta TO 45.00 (po przelewie).
        balanceOf(FROM).compareTo(new BigDecimal("75.00")) == 0
        balanceOf(TO).compareTo(new BigDecimal("45.00")) == 0
        historyFor("lab48-already-committed").size() == 2
    }

    private void openAccounts() {
        service.openAccount(FROM, "Alice", new BigDecimal("100.00"), "PLN")
        service.openAccount(TO, "Bob", new BigDecimal("20.00"), "PLN")
    }

    private BigDecimal balanceOf(String number) {
        return jdbcTemplate.queryForObject(
                "select balance from bank_accounts where number = ?",
                BigDecimal,
                number)
    }

    private List<Map<String, Object>> historyFor(String requestId) {
        return jdbcTemplate.queryForList("""
                select account_number, type, amount
                from bank_transactions
                where transfer_request_id = ?
                order by account_number
                """, requestId).collect { row ->
            [account: row.account_number, type: row.type, amount: row.amount]
        }
    }
}