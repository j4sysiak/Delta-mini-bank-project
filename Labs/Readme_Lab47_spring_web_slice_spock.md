# Lab 47 — Spring Boot: test warstwy HTTP przez `@WebMvcTest`

## Kontekst projektu

Projekt używa Java 21, Spring Boot 3.4.5, Groovy dostarczanego przez
Gradle (`localGroovy()`) oraz Spocka 2.3 dla Groovy 3.0. Nie zmieniaj
tych wersji ani konfiguracji Gradle w tym labie.

Mini-bank ma już warstwy Controller → Service → Repository. Operacje
zapisujące w `BankAccountService` są objęte `@Transactional`; przelew
blokuje konta w uporządkowanej kolejności, zapisuje historię i obsługuje
idempotency key. Testy API w `BankAccountApiSpec` używają obecnie
`@SpringBootTest` i testowej bazy PostgreSQL.

W tym labie dodasz **test wycinka MVC**. Sprawdzisz mapowanie HTTP,
walidację DTO i `GlobalExceptionHandler`, zastępując serwis mockiem.
Test nie uruchomi JPA ani PostgreSQL. Nie zmieniaj kodu produkcyjnego,
istniejących testów ani konfiguracji Gradle — dodaj tylko wskazany test.

## 1. Specyfikacja wycinka MVC

Utwórz plik:
`src\test\groovy\com\delta\bank\api\controller\BankAccountControllerWebSpec.groovy`

```groovy
package com.delta.bank.api.controller

import com.delta.bank.application.service.BankAccountService
import com.delta.bank.bootstrap.DeltaApplication
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

@WebMvcTest(controllers = BankAccountController)
@ContextConfiguration(classes = DeltaApplication)
@Import(GlobalExceptionHandler)
class BankAccountControllerWebSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
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
```

`@WebMvcTest` konfiguruje elementy potrzebne do obsługi MVC, a
`@SpringBean` dostarcza mock `BankAccountService`. Dzięki temu testy
sprawdzają granicę HTTP i nie wykonują operacji w bazie.

`@ContextConfiguration(classes = DeltaApplication)` wskazuje klasę
startową projektu, która znajduje się w pakiecie `com.delta.bank.bootstrap`.
`@Import(GlobalExceptionHandler)` zapewnia użycie istniejącego mapowania
wyjątków w odpowiedzi HTTP.

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.api.controller.BankAccountControllerWebSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.api.controller.BankAccountControllerWebSpec"
```

Ten test wycinka MVC nie powinien wymagać uruchomionej testowej bazy
PostgreSQL. Jeśli kontekst mimo to próbuje tworzyć repozytoria JPA,
zatrzymaj się i sprawdź konfigurację wycinka — nie wyłączaj po cichu
Flyway ani nie zmieniaj konfiguracji dla całego projektu.

## Eksperymenty

1. Zmień `number` na pusty tekst i usuń `@SpringBean`. Co mówi błąd
   o brakującym bean'ie, a co mówi odpowiedź HTTP o walidacji?
2. Dodaj test niepoprawnego JSON-a (np. brak zamykającego cudzysłowu).
   Czy błąd przechodzi przez ten sam handler co `@Valid`?
3. Dodaj przypadek, w którym `service.openAccount` rzuca
   `IllegalArgumentException`. Sprawdź status i `errorCode` odpowiedzi.
4. Porównaj ten test z `BankAccountApiSpec`: które testy korzystają
   z prawdziwej bazy, a które izolują warstwę webową?
5. Dodaj test dla `GET /accounts/{number}`. Zamockuj `service.find`
   i sprawdź, że kontroler mapuje wynik na `AccountResponse`.

## Jak o tym opowiedzieć

- **`@SpringBootTest`** ładuje szeroki kontekst aplikacji. Jest właściwe
  dla testów integracyjnych, ale zwykle wolniejsze i może wymagać bazy.
- **`@WebMvcTest`** testuje webowy wycinek aplikacji: routing, serializację,
  walidację MVC i obsługę wyjątków. Zależności poza tą warstwą, np. serwis,
  zastępuje się mockami.
- **`MockMvc`** wysyła żądania do warstwy MVC bez uruchamiania prawdziwego
  serwera HTTP.
- Test MVC nie dowodzi, że transakcja, JPA ani SQL działają poprawnie.
  Do tego służą osobne testy integracyjne.

## Pytania kontrolne

1. Co dokładnie testuje `@WebMvcTest`, a czego nie testuje?
2. Dlaczego w pierwszym teście sprawdzamy jednocześnie odpowiedź HTTP
   i interakcję z serwisem?
3. Kiedy wybrać `@SpringBootTest`, a kiedy wycinek MVC?
4. Dlaczego walidacja żądania powinna odrzucić niepoprawne dane przed
   wywołaniem serwisu?

## Do zapamiętania

- Testuj warstwę w izolacji, ale zachowaj osobne testy integracyjne całego przepływu.
- `@WebMvcTest` + `MockMvc` sprawdza HTTP bez prawdziwego serwera i bazy.
- Mock serwisu pozwala zweryfikować granicę Controller → Service.
- W projekcie pozostają Java 21, Spring Boot 3.4.5, Groovy z Gradle
  i Spock 2.3 dla Groovy 3.0.
