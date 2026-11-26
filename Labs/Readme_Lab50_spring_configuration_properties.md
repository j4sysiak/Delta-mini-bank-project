# Lab 50 — Spring Boot: typowana konfiguracja przez `@ConfigurationProperties`

## Dlaczego ten temat?

W Labach 47–49 testowałeś warstwę HTTP, granice transakcji i serwis.
Teraz dodasz do mini-banku typowaną konfigurację: maksymalną kwotę
przelewu i domyślną walutę. Wartości będą pochodziły z `application.yml`,
a Spring zbindował je do klasy Java i zwaliduje przy starcie.

Poznasz:

- `@ConfigurationProperties` jako alternatywę dla wielu adnotacji `@Value`,
- rejestrację klasy konfiguracyjnej w aplikacji,
- walidację konfiguracji przez Jakarta Bean Validation,
- testowanie bindowania bez uruchamiania całego Spring Boot i PostgreSQL.

Zachowujemy Java 21, Spring Boot 3.4.5, Groovy dostarczane przez
`localGroovy()` i Spock 2.3 dla Groovy 3.0. Nie zmieniaj wersji
zależności ani wrappera Gradle.

## 1. Klasa typowanej konfiguracji

Utwórz:
`src\main\java\com\delta\bank\config\BankTransferProperties.java`

```java
package com.delta.bank.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "bank.transfer")
public class BankTransferProperties {

    @NotNull
    @Positive
    private BigDecimal maxAmount = new BigDecimal("5000.00");

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    private String defaultCurrency = "PLN";

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }
}
```

To JavaBean, więc Spring może zbindować właściwości przez settery.
Wartości inicjalizujące są domyślne, gdy dana właściwość nie zostanie
podana w konfiguracji.

## 2. Rejestracja i konfiguracja YAML

W `src\main\java\com\delta\bank\bootstrap\DeltaApplication.java`
dodaj import i adnotację:

```java
import com.delta.bank.config.BankTransferProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
```

```java
@EnableConfigurationProperties(BankTransferProperties.class)
```

Umieść ją przy pozostałych adnotacjach na `DeltaApplication`.
Nie usuwaj istniejących adnotacji.

W `src\main\resources\application.yml` dodaj sekcję najwyższego poziomu:

```yaml
bank:
  transfer:
    max-amount: 5000.00
    default-currency: PLN
```

Nazwy `max-amount` i `default-currency` w YAML są automatycznie
mapowane na `maxAmount` i `defaultCurrency` w Javie (relaxed binding).
W tym labie konfiguracja jest udostępniona jako bean; w kolejnym kroku
można ją wstrzyknąć do polityki lub serwisu, który egzekwuje limit.

## 3. Test bindowania i walidacji bez bazy

Utwórz:
`src\test\groovy\com\delta\bank\config\BankTransferPropertiesSpec.groovy`

```groovy
package com.delta.bank.config

import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import spock.lang.Specification

class BankTransferPropertiesSpec extends Specification {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesTestConfiguration)
                    .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration))

    def "binds YAML-style property names to typed values"() {
        when:
        contextRunner
                .withPropertyValues(
                        "bank.transfer.max-amount=2500.75",
                        "bank.transfer.default-currency=EUR")
                .run { context ->
                    assert !context.hasFailed()
                    def properties = context.getBean(BankTransferProperties)
                    assert properties.maxAmount == new BigDecimal("2500.75")
                    assert properties.defaultCurrency == "EUR"
                }

        then:
        noExceptionThrown()
    }

    def "uses defaults when configuration values are absent"() {
        when:
        contextRunner.run { context ->
            assert !context.hasFailed()
            def properties = context.getBean(BankTransferProperties)
            assert properties.maxAmount == new BigDecimal("5000.00")
            assert properties.defaultCurrency == "PLN"
        }

        then:
        noExceptionThrown()
    }

    def "invalid configuration prevents the context from starting"() {
        when:
        contextRunner
                .withPropertyValues(
                        "bank.transfer.max-amount=0",
                        "bank.transfer.default-currency=pln")
                .run { context ->
                    assert context.hasFailed()
                    assert context.startupFailure != null
                }

        then:
        noExceptionThrown()
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(BankTransferProperties)
    static class PropertiesTestConfiguration {
    }
}
```

`ApplicationContextRunner` uruchamia mały kontekst zawierający tylko
klasę właściwości i walidację. Test nie dziedziczy po
`BaseIntegrationSpec`, nie skanuje `DeltaApplication` i nie łączy się
z PostgreSQL.

Test poprawnej konfiguracji sprawdza binding, test wartości domyślnych
potwierdza zachowanie bez ustawień, a test niepoprawnych wartości
sprawdza, że kontekst nie wystartuje. Nie sprawdzamy tekstu wyjątku,
bo szczegóły komunikatu zależą od wersji walidatora.

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.config.BankTransferPropertiesSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.config.BankTransferPropertiesSpec"
```

Ten test nie wymaga Docker Desktop ani uruchomionej aplikacji.

## Eksperymenty

1. Usuń `@EnableConfigurationProperties` z testowej konfiguracji.
   Jaki błąd pojawia się przy pobieraniu beana? Przywróć adnotację.
2. Usuń `@Validated` z klasy właściwości i uruchom test niepoprawnej
   konfiguracji. Dlaczego same adnotacje `@Positive` i `@Pattern`
   nie wystarczą?
3. Zmień `max-amount` w `application.yml`, a potem ustaw tę samą
   właściwość przez zmienną środowiskową
   `BANK_TRANSFER_MAX_AMOUNT`. Zbadaj, która wartość wygrywa.
4. Dodaj `min-amount` z walidacją `@Positive` i sprawdź, czy
   `min-amount` nie przekracza `max-amount`. Do reguły porównującej
   dwie właściwości potrzebny będzie własny walidator klasowy.
5. Zastanów się, jak użyć `BankTransferProperties` w serwisie, aby
   przelew ponad limit był odrzucany przed pobraniem i blokowaniem kont.
   Dodaj test jednostkowy tej reguły w odpowiednim kolejnym ćwiczeniu.

## Jak o tym opowiedzieć

`@ConfigurationProperties` wiąże powiązane ustawienia z pliku
konfiguracyjnego ze spójnym typem Java. Jest czytelniejsze i łatwiejsze
do walidacji, gdy ustawień jest kilka lub więcej, niż rozproszone
`@Value("${...}")`.

Konfiguracja z walidacją fail-fast sprawia, że aplikacja nie uruchamia
się z niepoprawnymi limitami. Wartości można dostarczać przez YAML,
profile Springa, argumenty uruchomieniowe lub zmienne środowiskowe.
Konfiguracja sama nie egzekwuje reguł biznesowych — serwis musi jej
użyć w odpowiednim miejscu.

## Pytania kontrolne

1. Co robi `@EnableConfigurationProperties`?
2. Jak Spring mapuje `max-amount` na `maxAmount`?
3. Czym różni się walidacja konfiguracji przy starcie od walidacji żądania HTTP?
4. Dlaczego niepoprawny limit warto wykryć przed przyjęciem ruchu?
5. Czy samo dodanie właściwości limitu do YAML ogranicza przelewy? Dlaczego?

## Do zapamiętania

- `@ConfigurationProperties` grupuje ustawienia w typowanym obiekcie.
- `@Validated` uruchamia Bean Validation dla zbindowanych właściwości.
- `ApplicationContextRunner` pozwala testować konfigurację w małym kontekście.
- Wartość konfiguracyjna nie egzekwuje się sama — musi zostać użyta przez logikę aplikacji.
