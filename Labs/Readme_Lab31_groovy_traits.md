# Lab 31 — Groovy: traits i współdzielenie zachowania

## Dlaczego ten temat?

W poprzednich labach używałeś klas Groovy, closures, builderów i testów
Spocka. Teraz poznasz **traits** — mechanizm pozwalający współdzielić
zachowanie między klasami bez kopiowania tych samych metod.

Zbudujesz trait audytowy, który:

- dostarcza gotową metodę formatującą wpis,
- wymaga od klasy implementującej dostarczenia zegara i nazwy źródła,
- może być użyty przez różne serwisy bez powielania logiki.

Lab przypomina również, jak wstrzyknąć `Clock`, aby testować czas
deterministycznie — temat ten pojawił się już w Labie 7.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. Czym jest trait?

Trait to element Groovy służący do komponowania zachowania. Może zawierać:

- gotowe metody,
- metody abstrakcyjne, które klasa implementująca musi dostarczyć,
- właściwości i metody pomocnicze.

Klasa korzysta z traitu przez `implements`:

```groovy
class BankAuditService implements OperationAudit {
    // implementuje wymagane metody
}
```

W odróżnieniu od klasy bazowej, klasa może implementować wiele traits.
Trait nie jest jednak automatycznie lepszy od klasy bazowej ani interfejsu:
wybierz go wtedy, gdy chcesz składać zachowanie niezależnie od hierarchii
dziedziczenia.

## 2. Gotowa metoda i wymagany kontrakt

Trait może mieć metodę z implementacją:

```groovy
String auditLine(String action) {
    // gotowe zachowanie współdzielone przez implementujące klasy
}
```

Może też wymagać od klasy implementującej metod:

```groovy
abstract Clock auditClock()
abstract String auditSource()
```

W tym labie `auditLine` będzie wspólnym zachowaniem, a konkretna klasa
zdecyduje, jaki zegar i identyfikator źródła wykorzystać.

## 3. Trait audytowy

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\traits
```

Utwórz plik `OperationAudit.groovy`:

```groovy
package com.delta.bank.lab.groovy.traits

import java.time.LocalDateTime
import java.time.Clock
import java.util.Objects

trait OperationAudit {

    abstract Clock auditClock()

    abstract String auditSource()

    String auditLine(String action) {
        Objects.requireNonNull(action, "action must not be null")

        if (action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank")
        }

        String source = auditSource()
        if (source == null || source.isBlank()) {
            throw new IllegalStateException("auditSource must not be blank")
        }

        String normalizedAction = action.strip()
        LocalDateTime timestamp = LocalDateTime.now(auditClock())

        return "${timestamp}|${source}|${normalizedAction}"
    }
}
```

Trait nie przechowuje tutaj swojego stanu. Zegar i źródło dostarcza
obiekt, który implementuje wymagane metody. Gotowa metoda `auditLine`
korzysta z tego kontraktu.

## 4. Klasa implementująca trait

Utwórz plik `BankAuditService.groovy` w tym samym katalogu:

```groovy
package com.delta.bank.lab.groovy.traits

import java.time.Clock
import java.util.Objects

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
```

Klasa dostarcza dwie metody wymagane przez trait. Nie implementuje
`auditLine` — korzysta z gotowej implementacji `OperationAudit`.

Wywołanie:

```groovy
def service = new BankAuditService(Clock.systemUTC(), "transfer-service")
def line = service.auditLine("Transfer PLN-1001 -> PLN-1002")
```

W produkcyjnym przykładzie `Clock.systemUTC()` odczytuje bieżący czas.
W testach użyjemy `Clock.fixed(...)`, aby wynik nie zależał od chwili
uruchomienia testu.

## 5. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\traits
```

Utwórz w nim `BankAuditServiceSpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.traits

import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class BankAuditServiceSpec extends Specification {

    private static Clock fixedClock() {
        Clock.fixed(
                Instant.parse("2026-03-04T10:15:30Z"),
                ZoneOffset.UTC
        )
    }

    def "uses trait behavior and the implementation's clock and source"() {
        given:
        def service = new BankAuditService(fixedClock(), "transfer-service")

        expect:
        service.auditLine(" Transfer PLN-1001 -> PLN-1002 ") ==
                "2026-03-04T10:15:30|transfer-service|Transfer PLN-1001 -> PLN-1002"
    }

    def "same trait behavior can be reused with another source"() {
        given:
        def service = new BankAuditService(fixedClock(), "deposit-service")

        expect:
        service.auditLine("Deposit PLN-1001") ==
                "2026-03-04T10:15:30|deposit-service|Deposit PLN-1001"
    }

    def "rejects a null action"() {
        given:
        def service = new BankAuditService(fixedClock(), "transfer-service")

        when:
        service.auditLine(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "action must not be null"
    }

    def "rejects a blank action"() {
        given:
        def service = new BankAuditService(fixedClock(), "transfer-service")

        when:
        service.auditLine(" \t ")

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "action must not be blank"
    }

    def "rejects a blank audit source"() {
        given:
        def service = new BankAuditService(fixedClock(), " ")

        when:
        service.auditLine("Deposit PLN-1001")

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "auditSource must not be blank"
    }

    def "rejects a null clock in the implementing class"() {
        when:
        new BankAuditService(null, "transfer-service")

        then:
        def exception = thrown(NullPointerException)
        exception.message == "clock must not be null"
    }
}
```

`Clock.fixed(...)` daje zawsze ten sam moment, dzięki czemu test sprawdza
dokładny tekst bez czekania i bez zależności od zegara systemowego.
Zwróć uwagę, że `LocalDateTime.now(clock)` używa strefy czasowej zegara.
W teście ustawiliśmy UTC przez `ZoneOffset.UTC`.

## 6. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.traits.BankAuditServiceSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.traits.BankAuditServiceSpec"
```

Test nie wymaga uruchomienia kontenera PostgreSQL.

## 7. Ćwiczenia

1. Dodaj do traitu metodę `auditEntryCountLabel(int count)`, która zwraca
   tekst `"Entries: <count>"`. Przetestuj wartość zero i liczbę dodatnią.
2. Dodaj drugą klasę implementującą `OperationAudit`, np.
   `AccountAuditService`, z inną wartością źródła. Nie kopiuj metody
   `auditLine`.
3. Dodaj do traitu metodę, która zwraca `LocalDateTime` zamiast od razu
   formatować tekst. Zmień `auditLine`, aby korzystała z tej metody.
4. Zrób eksperyment: usuń z implementującej klasy jedną z metod
   abstrakcyjnych i skompiluj projekt. Zobacz, na jakim etapie Groovy
   zgłasza brak wymaganego kontraktu.
5. Zastanów się, jak przetestować klasę z `Clock.systemUTC()` bez
   wstrzykiwanego zegara. Dlaczego wersja z `Clock` jest prostsza
   i deterministyczna?

## 8. Trait a interfejs i klasa bazowa

| Mechanizm | Typowe zastosowanie |
|---|---|
| interfejs | deklaracja kontraktu; Java i Groovy mogą go implementować |
| trait Groovy | składane zachowanie oraz wymagane metody |
| klasa bazowa | wspólny stan i implementacja w jednej hierarchii dziedziczenia |

Trait może być przydatny, gdy kilka niezależnych klas potrzebuje tej
samej implementacji. Jeżeli zachowanie wymaga istotnego, współdzielonego
stanu albo skomplikowanego cyklu życia, rozważ zwykłą współpracę z osobnym
serwisem zamiast ukrywać stan w traitach.

To ćwiczenie nie dotyka JPA. Nie zakładaj, że trait należy automatycznie
dodawać do encji — najpierw sprawdź, jak Groovy kompiluje trait i jak
zachowują się używane frameworki oraz narzędzia.

## Pytania kontrolne

1. Co może zawierać trait Groovy?
2. Które metody `OperationAudit` musi dostarczyć klasa implementująca?
3. Dlaczego `BankAuditService` nie musi implementować `auditLine`?
4. Jak `Clock.fixed(...)` pomaga pisać deterministyczne testy?
5. Czy trait jest tym samym co klasa bazowa?
6. Kiedy współdzielone zachowanie lepiej umieścić w osobnym serwisie?
7. Dlaczego trait w tym przykładzie nie przechowuje własnego stanu?

Do zapamiętania:

```text
trait               → składane zachowanie dla klas
metoda konkretna    → współdzielona implementacja
metoda abstrakcyjna → kontrakt do dostarczenia przez klasę
implements Trait    → dołączenie zachowania i kontraktu
Clock.fixed(...)   → deterministyczny czas w testach
```
