# Lab 43 — Chain of Responsibility: łańcuch walidacji przelewu

## Dlaczego ten temat?

W Labach 39–42 poznałeś wzorce kreacyjne, behawioralne i strukturalne.
Teraz wracamy do wzorców behawioralnych: **Chain of Responsibility**
(łańcuch odpowiedzialności) pozwala przekazać żądanie przez szereg
obiektów, z których każdy wykonuje własne zadanie.

Zbudujesz łańcuch walidacji przelewu. Kolejne kroki sprawdzą konta,
kwotę i limit. Pierwszy błąd przerwie przetwarzanie, a poprawny przelew
przejdzie przez cały łańcuch.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj istniejącego kodu
ani konfiguracji Gradle. Ćwiczenie działa bez Springa, bazy i sieci;
pokazuje walidację, **nie wykonuje prawdziwego przelewu**.

## 1. Dane przelewu

Plik: `src\main\java\com\delta\bank\lab\patterns\chain\TransferRequest.java`

```java
package com.delta.bank.lab.patterns.chain;

import java.math.BigDecimal;
import java.util.Objects;

public record TransferRequest(String from, String to, BigDecimal amount) {

    public TransferRequest {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
    }
}
```

Record pilnuje, by pola nie były `null`. Pozostałe reguły umieścimy
w osobnych walidatorach. Nie sprawdzamy tu, czy konta istnieją, ani
czy nadawca ma wystarczające saldo.

## 2. Interfejs i łączenie ogniw

Plik: `src\main\java\com\delta\bank\lab\patterns\chain\TransferValidator.java`

```java
package com.delta.bank.lab.patterns.chain;

import java.util.Objects;

@FunctionalInterface
public interface TransferValidator {

    void validate(TransferRequest request);

    default TransferValidator andThen(TransferValidator next) {
        Objects.requireNonNull(next, "next");
        return request -> {
            validate(request);
            next.validate(request);
        };
    }
}
```

`andThen` tworzy nowy walidator łączący dwa kroki. Jeśli pierwszy rzuci
wyjątek, drugi nie zostanie wywołany. Oryginalne walidatory nie są
modyfikowane, a kolejność łączenia ma znaczenie.

## 3. Ogniwa łańcucha

Plik: `src\main\java\com\delta\bank\lab\patterns\chain\AccountValidator.java`

```java
package com.delta.bank.lab.patterns.chain;

import java.util.Objects;

public final class AccountValidator implements TransferValidator {

    @Override
    public void validate(TransferRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.from().isBlank() || request.to().isBlank()) {
            throw new IllegalArgumentException("account must not be blank");
        }
        if (request.from().equals(request.to())) {
            throw new IllegalArgumentException("accounts must be different");
        }
    }
}
```

Plik: `src\main\java\com\delta\bank\lab\patterns\chain\AmountValidator.java`

```java
package com.delta.bank.lab.patterns.chain;

import java.util.Objects;

public final class AmountValidator implements TransferValidator {

    @Override
    public void validate(TransferRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
```

Plik: `src\main\java\com\delta\bank\lab\patterns\chain\LimitValidator.java`

```java
package com.delta.bank.lab.patterns.chain;

import java.math.BigDecimal;
import java.util.Objects;

public final class LimitValidator implements TransferValidator {

    private final BigDecimal limit;

    public LimitValidator(BigDecimal limit) {
        this.limit = Objects.requireNonNull(limit, "limit");
        if (limit.signum() <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
    }

    @Override
    public void validate(TransferRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.amount().compareTo(limit) > 0) {
            throw new IllegalArgumentException("transfer exceeds limit");
        }
    }
}
```

Każda klasa odpowiada za jedną grupę reguł. `compareTo` porównuje
wartość kwot bez uzależniania wyniku od skali `BigDecimal`.

## 4. Złożenie łańcucha

Plik: `src\main\java\com\delta\bank\lab\patterns\chain\TransferValidation.java`

```java
package com.delta.bank.lab.patterns.chain;

import java.math.BigDecimal;

public final class TransferValidation {

    private TransferValidation() {}

    public static TransferValidator standard(BigDecimal limit) {
        return new AccountValidator()
                .andThen(new AmountValidator())
                .andThen(new LimitValidator(limit));
    }
}
```

Przykład użycia:

```java
TransferValidator validator = TransferValidation.standard(new BigDecimal("1000.00"));
validator.validate(new TransferRequest("PLN-1", "PLN-2", new BigDecimal("25.00")));
```

W tym wariancie każde ogniwo, które nie zgłosi błędu, przekazuje żądanie
dalej; wyjątek przerywa cały łańcuch. W klasycznym ujęciu handler może
także sam zdecydować, czy obsłuży żądanie i zakończy przetwarzanie,
czy przekaże je następnemu. Ta wersja jest **łańcuchem walidacji**,
a nie przykładem wyboru jednego handlera.

## 5. Specyfikacja Spocka

Plik: `src\test\groovy\com\delta\bank\lab\patterns\chain\ChainOfResponsibilitySpec.groovy`

```groovy
package com.delta.bank.lab.patterns.chain

import spock.lang.Specification
import spock.lang.Unroll

class ChainOfResponsibilitySpec extends Specification {

    def "valid transfer passes the entire chain including the limit boundary"() {
        given:
        def validator = TransferValidation.standard(new BigDecimal("100.00"))
        def request = new TransferRequest("PLN-1", "PLN-2", new BigDecimal("100.0"))

        when:
        validator.validate(request)

        then:
        noExceptionThrown()
    }

    @Unroll
    def "invalid request is rejected: #reason"() {
        given:
        def validator = TransferValidation.standard(new BigDecimal("100.00"))

        when:
        validator.validate(new TransferRequest(from, to, new BigDecimal(amount)))

        then:
        def error = thrown(IllegalArgumentException)
        error.message == reason

        where:
        from    | to      | amount   || reason
        " "     | "PLN-2" | "1.00"   || "account must not be blank"
        "PLN-1" | "PLN-1" | "1.00"   || "accounts must be different"
        "PLN-1" | "PLN-2" | "0.00"   || "amount must be positive"
        "PLN-1" | "PLN-2" | "-1.00"  || "amount must be positive"
        "PLN-1" | "PLN-2" | "100.01" || "transfer exceeds limit"
    }

    def "validators run in the configured order"() {
        given:
        def calls = []
        TransferValidator first = { request -> calls << "first" }
        TransferValidator second = { request -> calls << "second" }
        TransferValidator third = { request -> calls << "third" }
        def chain = first.andThen(second).andThen(third)
        def request = new TransferRequest("A", "B", BigDecimal.ONE)

        when:
        chain.validate(request)

        then:
        calls == ["first", "second", "third"]
    }

    def "a failing validator prevents the following validator from running"() {
        given:
        def next = Mock(TransferValidator)
        def chain = new AmountValidator().andThen(next)
        def request = new TransferRequest("A", "B", BigDecimal.ZERO)

        when:
        chain.validate(request)

        then:
        thrown(IllegalArgumentException)
        0 * next.validate(_)
    }

    def "null next validator is rejected while building the chain"() {
        when:
        new AccountValidator().andThen(null)

        then:
        thrown(NullPointerException)
    }
}
```

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.patterns.chain.ChainOfResponsibilitySpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.patterns.chain.ChainOfResponsibilitySpec"
```

## Eksperymenty

1. Dodaj walidator wymuszający maksymalnie dwie cyfry po przecinku.
   Przetestuj m.in. `1.001` i zastanów się, jak potraktować `1.000`.
2. Dodaj własny wyjątek walidacji z kodem błędu. Sprawdź w teście kod,
   zamiast uzależniać klienta od dokładnej treści komunikatu.
3. Zmień kolejność walidatorów. Który błąd zobaczysz, gdy jednocześnie
   konto jest puste i kwota jest ujemna?
4. Zbuduj drugi łańcuch z innym limitem lub dodatkowym krokiem.
   Czy musisz zmieniać istniejące klasy walidatorów?
5. Zaprojektuj alternatywną wersję zbierającą **wszystkie** błędy
   zamiast przerywania przy pierwszym. Jak zmieniłby się kontrakt?

## Jak odpowiedzieć na rozmowie

**Chain of Responsibility** przekazuje żądanie przez łańcuch handlerów,
oddzielając nadawcę od szczegółów obsługi. Handler może obsłużyć
żądanie, przekazać je dalej albo przerwać przetwarzanie — zależnie od
kontraktu. Przykłady to filtry HTTP i walidacja wieloetapowa.

Zaletą jest możliwość składania i zmiany kolejności kroków. Kosztem są
trudniejsze śledzenie przepływu, znaczenie kolejności oraz ryzyko, że
żądanie nie zostanie obsłużone. Warto ustalić, czy łańcuch zatrzymuje
się na pierwszym błędzie, czy zbiera wszystkie błędy.

**Porównanie:** Strategy wybiera jeden algorytm, Decorator opakowuje
obiekt, by dodać mu zachowanie, a Chain of Responsibility organizuje
przekazywanie żądania między kolejnymi handlerami. Struktury mogą być
podobne — decyduje intencja.

## Pytania kontrolne

1. Co w tym przykładzie przerywa łańcuch i jak test wykazuje, że kolejny krok się nie wykonał?
2. Dlaczego kolejność walidatorów wpływa na błąd widoczny klientowi?
3. Jak odróżnisz wariant „wszystkie kroki” od wariantu „pierwszy handler, który obsłuży żądanie”?
4. Jakie są kompromisy między zgłaszaniem pierwszego błędu a zbieraniem wszystkich błędów?

## Do zapamiętania

- Łańcuch dzieli obsługę żądania na niezależne kroki.
- Kontrakt musi jasno określać przekazywanie dalej i zatrzymanie.
- Wzorzec nie zastępuje walidacji po stronie serwera ani kontroli uprawnień.
