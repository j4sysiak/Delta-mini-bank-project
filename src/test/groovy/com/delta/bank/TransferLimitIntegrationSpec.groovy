package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import com.delta.bank.bootstrap.DeltaApplication
import com.delta.bank.config.BankTransferProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Integration tests for configured bank transfer limits.
 *
 * Verifies that transfers exceeding the configured maximum amount
 * are rejected without modifying account balances or transaction data.
 *
 * „Integration tests” to **testy integracyjne** — sprawdzają, czy kilka elementów aplikacji współpracuje poprawnie, a nie tylko pojedyncza metoda w izolacji.

 W tym teście uruchamiany jest kontekst Springa, wczytywany jest skonfigurowany limit przelewu,
 a operacja przelewu sprawdza również zapisane dane w bazie.
 Test weryfikuje, że przelew ponad limit zostaje odrzucony i nie zmienia sald ani danych transakcji.
 */
@SpringBootTest(
        classes = DeltaApplication,
        properties = ["bank.transfer.max-amount=100.00"])
class TransferLimitIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    @Autowired
    BankTransferProperties properties

    def "skonfigurowany limit odrzuca przelew bez zmiany zapisanych danych"() {
        given:
        service.openAccount("PLN-5101", "Alice", new BigDecimal("200.00"), "PLN")
        service.openAccount("PLN-5102", "Bob", new BigDecimal("20.00"), "PLN")

        expect:
        properties.maxAmount.compareTo(new BigDecimal("100.00")) == 0

        when:
        // transfer z kwotą powyżej limitu (100.01) powinien zostać odrzucony, a salda kont i dane transakcji nie powinny ulec zmianie
        service.transfer("lab51-reject", "PLN-5101", "PLN-5102", new BigDecimal("100.01"))

        then:
        thrown(IllegalArgumentException)
        jdbcTemplate.queryForObject(
                "select balance from bank_accounts where number = ?",
                BigDecimal, "PLN-5101").compareTo(new BigDecimal("200.00")) == 0
        jdbcTemplate.queryForObject(
                "select balance from bank_accounts where number = ?",
                BigDecimal, "PLN-5102").compareTo(new BigDecimal("20.00")) == 0
        jdbcTemplate.queryForObject(
                "select count(*) from bank_transactions where transfer_request_id = ?",
                Long, "lab51-reject") == 0L
    }
}