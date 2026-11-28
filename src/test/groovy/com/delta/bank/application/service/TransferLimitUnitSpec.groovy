package com.delta.bank.application.service

import com.delta.bank.config.BankTransferProperties
import com.delta.bank.domain.AccountTransactionEntity
import com.delta.bank.domain.AccountTransactionRepository
import com.delta.bank.domain.BankAccountEntity
import com.delta.bank.domain.BankAccountRepository
import com.delta.bank.domain.TransactionType
import spock.lang.Specification
import spock.lang.Unroll

class TransferLimitUnitSpec extends Specification {

    BankAccountRepository accounts = Mock()
    AccountTransactionRepository transactions = Mock()
    BankTransferProperties properties = new BankTransferProperties()
    BankAccountService service

    def setup() {
        properties.maxAmount = new BigDecimal("100.00")
        service = new BankAccountService(accounts, transactions, properties)
    }

    def "nowy przelew powyżej limitu (100.01) jest odrzucany bez blokowania lub zapisywania kont"() {
        when:
        service.transfer("limit-reject", "PLN-1", "PLN-2", new BigDecimal("100.01"))

        then:
        def error = thrown(IllegalArgumentException)
        error.message == "Transfer amount exceeds configured limit"

        // ten kontrakt potwierdza, że serwis nie próbuje zapisywać danych w scenariuszu odrzuconym.
        // sprawdza, że nie wywołano żadnej metody na żadnym z tych repozytoriów
        // w service jak dojdzie do próby wywołania tej metody, to ona nie zostanie wywołana
        // od razu zwracamy Optional.empty() bo nie ma duplikatu, ale potem serwis odrzuca przelew bo kwota jest powyżej limitu
        1 * transactions.findFirstByTransferRequestIdAndAccountNumberAndType(
                "limit-reject", "PLN-1", TransactionType.TRANSFER_OUT) >> Optional.empty()
        0 * accounts._
        0 * transactions.save(_)
    }

    @Unroll
    def "nowy przelew o kwocie #amount jest dozwolony przy lub poniżej limitu"() {
        given:
        def from = new BankAccountEntity("PLN-1", "Alice", new BigDecimal("200.00"), "PLN")
        def to = new BankAccountEntity("PLN-2", "Bob", BigDecimal.ZERO, "PLN")

        when:
        // `amount` to jest zmienna, która przyjmuje wartości z listy w sekcji where.
        boolean executed = service.transfer("limit-allow", "PLN-1", "PLN-2", amount)

        then:
        1 * transactions.findFirstByTransferRequestIdAndAccountNumberAndType(
                "limit-allow", "PLN-1", TransactionType.TRANSFER_OUT) >> Optional.empty()

        // Wymusza dokładnie jedno wywołanie metody wyszukującej oba konta z blokadą w odpowiedniej kolejności.
        /*
        1 * — metoda musi zostać wywołana dokładnie raz.
              Argumentem jest lista numerów kont w kolejności: konto źródłowe "PLN-1", potem docelowe "PLN-2".
              >> [from, to] — to jest taki zapis, który mówi, że metoda findAllForUpdateOrderByNumber zwraca listę kont [from, to] czyli wcześniej utworzone obiekty kont.
        findAllForUpdateOrderByNumber — to jest nazwa metody w repozytorium, która wyszukuje konta z blokadą w kolejności numerów kont.
        Typ zwracany: List<BankAccountEntity> to będzie właśnie lista BankAccountEntity [from, to] czyli wcześniej utworzone obiekty kont..
        */
        1 * accounts.findAllForUpdateOrderByNumber(["PLN-1", "PLN-2"]) >> [from, to]  // Mock zwraca konto źródłowe i docelowe utworzone w sekcji given.

        1 * accounts.save(from)  // to oznacza, że dojdzie do zapisu konta  (BankAccountEntity) from
        1 * accounts.save(to)    // to oznacza, że dojdzie do zapisu konta  (BankAccountEntity) to
        2 * transactions.save(_) // i dojdzie do zapisu dwóch transakcji (AccountTransactionEntity) — jednej dla konta źródłowego i jednej dla konta docelowego
        executed
        from.balance.compareTo(new BigDecimal("200.00").subtract(amount)) == 0
        to.balance.compareTo(amount) == 0

        where:
        // Wartość 100.00 jest granicą, więc przypadek 99.99 i 100.00 powinny zostać zaakceptowane.
        // Dla każdej wartości amount test zostanie uruchomiony osobno.
        // czyli beda 2 uruchomienia testu: jeden z amount = 99.99 i drugi z amount = 100.00
        amount << [new BigDecimal("99.99"), new BigDecimal("100.0")]
    }

    def "ukończony duplikat pozostaje duplikatem powyżej bieżącego limitu"() {
        given:
        def existing = new AccountTransactionEntity(
                "PLN-1",
                TransactionType.TRANSFER_OUT,
                new BigDecimal("150.00"),
                "PLN",
                "Transfer to PLN-2",
                "limit-duplicate"
        )

        when:
        boolean executed = service.transfer(
                "limit-duplicate", "PLN-1", "PLN-2", new BigDecimal("150.00"))

        then:
        // >> Optional.of(existing): istnieje już transakcja o tym samym transfer_request_id i account_number i type, więc serwis traktuje to jako duplikat
        // i nie zapisuje kont ani nowej transakcji.
        1 * transactions.findFirstByTransferRequestIdAndAccountNumberAndType(
                "limit-duplicate", "PLN-1", TransactionType.TRANSFER_OUT) >> Optional.of(existing)

        0 * accounts._  // i nie bedzie zapisu kont, bo to jest duplikat
        0 * transactions.save(_)  // i nie bedzie zapisu nowej transakcji, bo to jest duplikat
        !executed
    }
}