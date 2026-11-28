package com.delta.bank.application.service

import com.delta.bank.domain.AccountTransactionEntity
import com.delta.bank.domain.AccountTransactionRepository
import com.delta.bank.domain.BankAccountEntity
import com.delta.bank.domain.BankAccountRepository
import com.delta.bank.domain.TransactionType
import spock.lang.Specification
import com.delta.bank.config.BankTransferProperties

class BankAccountServiceUnitSpec extends Specification {

    // Mocki są zwykłymi obiektami Spocka — BankAccountService tworzysz bezpośrednio przez konstruktor,
    // więc Spring nie tworzy kontekstu ani proxy transakcyjnego.
    // W testach interakcje 0 * ... potwierdzają, że serwis nie próbuje zapisywać danych w scenariuszu odrzuconym.
    BankAccountRepository accountRepository = Mock()
    AccountTransactionRepository transactionRepository = Mock()

    BankAccountService service

    def setup() {
        service = new BankAccountService(accountRepository,
                transactionRepository,
                new BankTransferProperties())
    }

    def "wpłata (deposit) odrzuca kwotę niedodatnią przed dostępem do repozytoriów"() {
        when:
        service.deposit("req-unit-1", "PLN-1", BigDecimal.ZERO)

        then:
        thrown(IllegalArgumentException)
        // w tym scenariuszu nie zostanie wywołana żadna metoda na żadnym z tych repozytoriów
        //    — nie tylko save, ale również np. metody wyszukujące.
        0 * accountRepository._
        0 * transactionRepository._
    }

    def "duplikat żądania przelewu (transfer), zwraca false bez blokowania lub zapisywania kont"() {
        given:
        // ten blok tworzy obiekt AccountTransactionEntity tylko w pamięci testu i przekazuje go jako wynik mocka.
        // Ponieważ metoda findFirst... zwraca Optional.of(existing),
        // serwis rozpozna duplikat i nie wywoła transactionRepository.save(...).
        // Jeśli istnieje ta transakcja w bazie, mock odzwierciedla właśnie taki stan.
        def existing = new AccountTransactionEntity(
                "PLN-1",
                TransactionType.TRANSFER_OUT,
                new BigDecimal("25.00"),
                "PLN",
                "Transfer to PLN-2",
                "req-unit-2")

        when:
        boolean executed = service.transfer(
                "req-unit-2",
                "PLN-1",
                "PLN-2",
                new BigDecimal("25.00"))

        then:
        // Przy wywołaniu metoda zwróci Optional.of(existing)
        //   — istnieje już transakcja o podanych parametrach,
        //     więc test będzie traktował żądanie jako duplikat i nie zapisze kont ani nowej transakcji.
        1 * transactionRepository.findFirstByTransferRequestIdAndAccountNumberAndType(
                "req-unit-2", "PLN-1", TransactionType.TRANSFER_OUT
        ) >> Optional.of(existing)   //  W składni Spocka >> określa, co ma zwrócić mock:
                                     // czyli, wywołanie metody findFirstByTransferRequestIdAndAccountNumberAndType z tymi parametrami zwróci Optional.of(existing)
                                     // to jest taki "stub" — mock zwraca określoną wartość, ale nie wykonuje żadnej logiki.
                                     // to taki kontrakt: jeśli wywołasz tę metodę z tymi parametrami, to dostaniesz Optional.of(existing)
        0 * accountRepository._ // np. 0 * accountRepository.save(_)
        0 * transactionRepository.save(_)  // 0 * transactionRepository.save(_)
        !executed  // i się nic nie wykonało, bo metoda transfer zwróci false, a nie true
    }

    def "Przelew (transfer) przy niewystarczających środkach kończy się niepowodzeniem przed zapisaniem zmian"() {
        given:
        def from = new BankAccountEntity(
                "PLN-1", "Alice", new BigDecimal("10.00"), "PLN")
        def to = new BankAccountEntity(
                "PLN-2", "Bob", new BigDecimal("20.00"), "PLN")

        when:
        service.transfer("req-unit-3", "PLN-1", "PLN-2", new BigDecimal("25.00"))

        then:
        def error = thrown(IllegalStateException)
        error.message == "Insufficient funds for transfer"

        // Jeśli serwis wywoła tę metodę z podanymi argumentami,
        // mock zwróci pusty Optional, czyli nie znaleziono istniejącej transakcji — transfer nie jest duplikatem
        // to sie wykona raz, bo serwis sprawdza, czy istnieje już transakcja o tym samym requestId i accountNumber i type.
        1 * transactionRepository.findFirstByTransferRequestIdAndAccountNumberAndType(
                "req-unit-3", "PLN-1", TransactionType.TRANSFER_OUT
        ) >> Optional.empty()  // kotrakt: jeśli wywołasz tę metodę z tymi parametrami, to dostaniesz Optional.empty()
        // Jeśli serwis wywoła tę metodę z podanymi argumentami,
        // mock zwróci pusty Optional, czyli nie znaleziono istniejącej transakcji — transfer nie jest duplikatem

        1 * accountRepository.findAllForUpdateOrderByNumber(["PLN-1", "PLN-2"]) >> [from, to]  // kotrakt: jeśli wywołasz tę metodę z tymi parametrami, to dostaniesz listę [from, to]
        0 * accountRepository.save(_)  // kontrakt: nie zapisujemy żadnego konta, bo przelew się nie powiódł
        0 * transactionRepository.save(_)  // kontrakt: nie zapisujemy żadnej transakcji, bo przelew się nie powiódł

        from.balance.compareTo(new BigDecimal("10.00")) == 0
        to.balance.compareTo(new BigDecimal("20.00")) == 0
    }
}