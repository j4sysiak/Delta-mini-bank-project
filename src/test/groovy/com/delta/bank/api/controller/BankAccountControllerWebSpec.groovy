package com.delta.bank.api.controller

import com.delta.bank.application.service.BankAccountService
import com.delta.bank.domain.BankAccountEntity
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = BankAccountController)  // @WebMvcTest konfiguruje elementy potrzebne do obsługi MVC
@ContextConfiguration(classes = [BankAccountController, GlobalExceptionHandler])
@Import(GlobalExceptionHandler)
class BankAccountControllerWebSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    // @SpringBean pozwala wstrzyknąć mocka do kontekstu Springa, aby zastąpić prawdziwy bean
    // Dzięki temu testy sprawdzają granicę HTTP i nie wykonują operacji w bazie.
    // W tym przypadku mockujemy BankAccountService, aby kontroler mógł go używać, ale nie wykonywał rzeczywistych operacji biznesowych.
    // czyli nie uruchomimy logiki biznesowej ani nie będziemy odwoływać się do bazy danych.
    BankAccountService service = Mock()

    def "creates an account and maps the service result to HTTP response"() {
        given:
        def account = new BankAccountEntity(
                "PLN-9001", "Alice", new BigDecimal("150.00"), "PLN")

        when:
        def response = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "number": "PLN-9001",
                      "owner": "Alice",
                      "balance": "150.00",
                      "currency": "PLN"
                    }
                '''))

        then:
        1 * service.openAccount(
                "PLN-9001", "Alice", new BigDecimal("150.00"), "PLN") >> account

        and:
        response.andExpect(status().isCreated())
                .andExpect(jsonPath('$.number').value("PLN-9001"))
                .andExpect(jsonPath('$.owner').value("Alice"))
                .andExpect(jsonPath('$.balance').value(150.0))
                .andExpect(jsonPath('$.currency').value("PLN"))
    }

    def "Bean Validation rejects an invalid request before calling the service"() {
        when:
        def response = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "number": "",
                      "owner": "Alice",
                      "balance": "150.00",
                      "currency": "PLN"
                    }
                '''))

        then:
        0 * service._

        and:
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.status').value(400))
                .andExpect(jsonPath('$.errorCode').value("VALIDATION_ERROR"))
                .andExpect(jsonPath('$.message').value("Account number is required"))
    }

    def "business exception is translated to a conflict response"() {
        when:
        def response = mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "requestId": "req-web-1",
                      "fromAccount": "PLN-9001",
                      "toAccount": "PLN-9002",
                      "amount": "500.00"
                    }
                '''))

        then:
        1 * service.transfer(
                "req-web-1", "PLN-9001", "PLN-9002", new BigDecimal("500.00")) >>
                { throw new IllegalStateException("Insufficient funds for transfer") }

        and:
        response.andExpect(status().isConflict())
                .andExpect(jsonPath('$.status').value(409))
                .andExpect(jsonPath('$.errorCode').value("INSUFFICIENT_FUNDS"))
    }
}