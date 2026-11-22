# Lab 44 — Abstract Factory: rodziny powiązanych obiektów

## Dlaczego ten temat?

W Labie 39 tworzyłeś różne rodzaje kont za pomocą `AccountFactory`.
Teraz poznasz **Abstract Factory**, która udostępnia metody tworzenia
całej rodziny współpracujących obiektów.

Zbudujesz przykład powiadomienia o przelewie w dwóch formatach:
tekstowym i HTML. Każda fabryka utworzy pasującą parę: nagłówek
i treść. Klient będzie korzystał z interfejsów, bez wybierania
konkretnych klas podczas generowania powiadomienia.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj istniejącego kodu
ani konfiguracji Gradle. Ćwiczenie działa bez Springa, bazy i sieci;
generuje tekst, ale nie wysyła rzeczywistych powiadomień.

## 1. Dane powiadomienia

Plik: `src\main\java\com\delta\bank\lab\patterns\abstractfactory\TransferNotice.java`

```java
package com.delta.bank.lab.patterns.abstractfactory;

import java.math.BigDecimal;
import java.util.Objects;

public record TransferNotice(String recipient, BigDecimal amount) {

    public TransferNotice {
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(amount, "amount");
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be blank");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
```

## 2. Interfejsy produktów i abstrakcyjnej fabryki

Plik: `src\main\java\com\delta\bank\lab\patterns\abstractfactory\NoticeHeader.java`

```java
package com.delta.bank.lab.patterns.abstractfactory;

@FunctionalInterface
public interface NoticeHeader {
    String render();
}
```

Plik: `src\main\java\com\delta\bank\lab\patterns\abstractfactory\NoticeBody.java`

```java
package com.delta.bank.lab.patterns.abstractfactory;

@FunctionalInterface
public interface NoticeBody {
    String render(TransferNotice notice);
}
```

Plik: `src\main\java\com\delta\bank\lab\patterns\abstractfactory\NoticeFactory.java`

```java
package com.delta.bank.lab.patterns.abstractfactory;

public interface NoticeFactory {
    NoticeHeader createHeader();
    NoticeBody createBody();
}
```

`NoticeHeader` i `NoticeBody` to dwa rodzaje produktów. `NoticeFactory`
deklaruje tworzenie obu, nie wskazując konkretnego formatu.

## 3. Konkretne fabryki i rodziny produktów

Plik: `src\main\java\com\delta\bank\lab\patterns\abstractfactory\TextNoticeFactory.java`

```java
package com.delta.bank.lab.patterns.abstractfactory;

public final class TextNoticeFactory implements NoticeFactory {

    @Override
    public NoticeHeader createHeader() {
        return () -> "TRANSFER";
    }

    @Override
    public NoticeBody createBody() {
        return notice -> "To: " + notice.recipient()
                + ", amount: " + notice.amount().toPlainString() + " PLN";
    }
}
```

Plik: `src\main\java\com\delta\bank\lab\patterns\abstractfactory\HtmlNoticeFactory.java`

```java
package com.delta.bank.lab.patterns.abstractfactory;

public final class HtmlNoticeFactory implements NoticeFactory {

    @Override
    public NoticeHeader createHeader() {
        return () -> "<h1>Transfer</h1>";
    }

    @Override
    public NoticeBody createBody() {
        return notice -> "<p>To: " + escapeHtml(notice.recipient())
                + ", amount: " + notice.amount().toPlainString() + " PLN</p>";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
```

W Javie interfejsy funkcyjne pozwalają tutaj użyć lambd jako produktów.
Nie są one wymagane przez wzorzec: produktami mogłyby być także osobne
klasy `TextNoticeBody` i `HtmlNoticeBody`.

Przykład HTML używa prostego kodowania tekstu umieszczanego między
znacznikami. To nie jest uniwersalny mechanizm sanitizacji HTML ani
zamiennik biblioteki szablonów dla innych kontekstów, np. atrybutów.

## 4. Klient korzystający z fabryki

Plik: `src\main\java\com\delta\bank\lab\patterns\abstractfactory\NoticeRenderer.java`

```java
package com.delta.bank.lab.patterns.abstractfactory;

import java.util.Objects;

public final class NoticeRenderer {

    private final NoticeFactory factory;

    public NoticeRenderer(NoticeFactory factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    public String render(TransferNotice notice) {
        Objects.requireNonNull(notice, "notice");
        NoticeHeader header = factory.createHeader();
        NoticeBody body = factory.createBody();
        return header.render() + "\n" + body.render(notice);
    }
}
```

Klient wie, że potrzebuje nagłówka i treści, ale nie zna konkretnych
implementacji. Fabrykę wybieramy na zewnątrz, np.:

```java
NoticeRenderer renderer = new NoticeRenderer(new TextNoticeFactory());
String message = renderer.render(new TransferNotice("Anna", new BigDecimal("25.00")));
```

Przy tworzeniu `TransferNotice` potrzebny jest import `java.math.BigDecimal`.
W prawdziwej aplikacji wybór fabryki może nastąpić np. podczas konfiguracji.

## 5. Specyfikacja Spocka

Plik: `src\test\groovy\com\delta\bank\lab\patterns\abstractfactory\AbstractFactorySpec.groovy`

```groovy
package com.delta.bank.lab.patterns.abstractfactory

import spock.lang.Specification
import spock.lang.Unroll

class AbstractFactorySpec extends Specification {

    @Unroll
    def "renderer uses a consistent #format product family"() {
        given:
        def renderer = new NoticeRenderer(factory)
        def notice = new TransferNotice("Anna", new BigDecimal("25.00"))

        expect:
        renderer.render(notice) == expected

        where:
        format | factory                 || expected
        "text" | new TextNoticeFactory() || "TRANSFER\nTo: Anna, amount: 25.00 PLN"
        "HTML" | new HtmlNoticeFactory() || "<h1>Transfer</h1>\n<p>To: Anna, amount: 25.00 PLN</p>"
    }

    def "HTML body escapes recipient text"() {
        given:
        def renderer = new NoticeRenderer(new HtmlNoticeFactory())
        def notice = new TransferNotice('<Anna & "Co">', BigDecimal.ONE)

        expect:
        renderer.render(notice) ==
                '<h1>Transfer</h1>\n<p>To: &lt;Anna &amp; &quot;Co&quot;&gt;, amount: 1 PLN</p>'
    }

    def "renderer asks the supplied factory for both products"() {
        given:
        def factory = Mock(NoticeFactory)
        NoticeHeader header = { -> "HEADER" }
        NoticeBody body = { notice -> "BODY: " + notice.recipient() }
        def renderer = new NoticeRenderer(factory)

        when:
        def result = renderer.render(new TransferNotice("Anna", BigDecimal.ONE))

        then:
        1 * factory.createHeader() >> header
        1 * factory.createBody() >> body
        result == "HEADER\nBODY: Anna"
    }

    def "renderer rejects a null factory"() {
        when:
        new NoticeRenderer(null)

        then:
        thrown(NullPointerException)
    }

    @Unroll
    def "notice rejects invalid data: #description"() {
        when:
        new TransferNotice(recipient, amount)

        then:
        thrown(IllegalArgumentException)

        where:
        description       | recipient | amount
        "blank recipient" | "  "      | BigDecimal.ONE
        "zero amount"     | "Anna"    | BigDecimal.ZERO
        "negative amount" | "Anna"    | new BigDecimal("-1.00")
    }
}
```

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.patterns.abstractfactory.AbstractFactorySpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.patterns.abstractfactory.AbstractFactorySpec"
```

## Eksperymenty

1. Dodaj trzecią rodzinę, np. Markdown, i kolejny wiersz tabeli testowej.
   Czy musisz zmieniać `NoticeRenderer`?
2. Dodaj nowy rodzaj produktu, np. `NoticeFooter`. Jakie interfejsy,
   fabryki i testy trzeba zmienić? Porównaj to z dodaniem nowej rodziny.
3. Zastąp lambdy osobnymi klasami produktów. Kiedy klasy mają więcej
   sensu niż lambdy?
4. Dodaj test z apostrofem w nazwie odbiorcy i sprawdź wynik HTML.
5. Zastanów się, jak ujednolicić format kwoty i walutę. W tym przykładzie
   `toPlainString()` zachowuje skalę wejściowego `BigDecimal`.

## Jak odpowiedzieć na rozmowie

**Abstract Factory** definiuje interfejs tworzenia rodzin powiązanych
obiektów bez wskazywania klientowi ich konkretnych klas. Przydaje się,
gdy komponenty powinny być używane razem, np. elementy interfejsu
użytkownika dla różnych motywów.

W tym labie rodziny to **tekst** i **HTML**, a rodzaje produktów to
nagłówek i treść. Wybór jednej fabryki dostarcza pasującą parę.
Zaletą jest wymiana całej rodziny bez zmiany klienta. Wadą jest większa
liczba abstrakcji, a dodanie nowego rodzaju produktu wymaga zwykle
zmiany interfejsu fabryki i jej implementacji.

**Porównanie z Labem 39:** tamtejsza statyczna metoda `create` ze
`switch` jest przykładem *Simple Factory*. Choć nagłówek Laba 39
nazywa ją Factory Method, nie jest to klasyczny **Factory Method**:
ten opiera się na metodzie tworzącej, której implementację dostarczają
podklasy twórcy. Abstract Factory skupia się natomiast na tworzeniu
**rodzin różnych, powiązanych produktów**.

## Pytania kontrolne

1. Jakie są dwie rodziny produktów w tym przykładzie, a jakie są dwa rodzaje produktów?
2. Dlaczego dodanie nowej rodziny jest łatwiejsze niż dodanie nowego rodzaju produktu?
3. Kto decyduje, czy aplikacja użyje `TextNoticeFactory`, czy `HtmlNoticeFactory`?
4. Czym Abstract Factory różni się od Simple Factory i Factory Method?
5. Kiedy dwie klasy i interfejs byłyby prostszym rozwiązaniem niż Abstract Factory?

## Do zapamiętania

- Abstract Factory tworzy **rodziny powiązanych produktów** przez wspólny interfejs.
- Klient pracuje z abstrakcjami; wybór konkretnej rodziny następuje poza nim.
- Wzorzec ułatwia dodawanie rodzin, ale utrudnia rozbudowę zestawu rodzajów produktów.
