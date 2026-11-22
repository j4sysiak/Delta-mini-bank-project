# Lab 45 — Prototype: tworzenie obiektów przez kopiowanie

## Dlaczego ten temat?

W Labach 39 i 44 poznawałeś fabryki, które tworzą obiekty na podstawie
wybranego typu lub rodziny. **Prototype** rozwiązuje inny problem:
tworzy nowy obiekt przez skopiowanie już skonfigurowanego egzemplarza.
To może być wygodne, gdy obiekt ma wiele ustawień albo jego inicjalizacja
jest kosztowna.

Zaimplementujesz prototyp przelewu z listą tagów. Kopia ma zachować
wartości pól, ale zmiany w jej mutowalnej liście nie mogą wpływać
na oryginał. Użyjemy jawnej metody kopiującej, a nie `Object.clone()`.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj istniejącego kodu
ani konfiguracji Gradle. Ćwiczenie działa bez Springa, bazy i sieci.

## 1. Interfejs Prototype

Plik: `src\main\java\com\delta\bank\lab\patterns\prototype\Prototype.java`

```java
package com.delta.bank.lab.patterns.prototype;

public interface Prototype<T> {
    T copy();
}
```

Interfejs określa, że obiekt potrafi zwrócić swoją kopię. Typ parametru
sprawia, że wynik `copy()` ma konkretny typ, bez rzutowania.

## 2. Prototyp przelewu

Plik: `src\main\java\com\delta\bank\lab\patterns\prototype\TransferDraft.java`

```java
package com.delta.bank.lab.patterns.prototype;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TransferDraft implements Prototype<TransferDraft> {

    private final String from;
    private final String to;
    private final BigDecimal amount;
    private final List<String> tags;

    public TransferDraft(String from, String to, BigDecimal amount, List<String> tags) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.amount = Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(tags, "tags");

        if (from.isBlank() || to.isBlank()) {
            throw new IllegalArgumentException("account must not be blank");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("accounts must be different");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        this.tags = new ArrayList<>(List.copyOf(tags));
    }

    private TransferDraft(TransferDraft source) {
        this.from = source.from;
        this.to = source.to;
        this.amount = source.amount;
        this.tags = new ArrayList<>(source.tags);
    }

    @Override
    public TransferDraft copy() {
        return new TransferDraft(this);
    }

    public void addTag(String tag) {
        Objects.requireNonNull(tag, "tag");
        if (tag.isBlank()) {
            throw new IllegalArgumentException("tag must not be blank");
        }
        tags.add(tag);
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public BigDecimal amount() {
        return amount;
    }

    public List<String> tags() {
        return List.copyOf(tags);
    }
}
```

Konstruktor kopiuje przekazaną listę, więc późniejsze zmiany listy
wejściowej nie zmienią obiektu. Konstruktor kopiujący tworzy nową listę,
czyli `copy()` nie współdzieli mutowalnego stanu z oryginałem.
`BigDecimal` i `String` są niemutowalne, dlatego można bezpiecznie
skopiować ich referencje.

## 3. Specyfikacja Spocka

Plik: `src\test\groovy\com\delta\bank\lab\patterns\prototype\PrototypeSpec.groovy`

```groovy
package com.delta.bank.lab.patterns.prototype

import spock.lang.Specification
import spock.lang.Unroll

class PrototypeSpec extends Specification {

    def "copy preserves values but is a distinct object"() {
        given:
        def original = new TransferDraft(
                "PLN-1", "PLN-2", new BigDecimal("25.00"), ["rent", "monthly"])

        when:
        def copy = original.copy()

        then:
        !copy.is(original)
        copy.from() == original.from()
        copy.to() == original.to()
        copy.amount().compareTo(original.amount()) == 0
        copy.tags() == original.tags()
    }

    def "copy has an independent mutable tags list"() {
        given:
        def original = new TransferDraft("PLN-1", "PLN-2", BigDecimal.TEN, ["rent"])

        when:
        def copy = original.copy()
        copy.addTag("urgent")

        then:
        copy.tags() == ["rent", "urgent"]
        original.tags() == ["rent"]
    }

    def "constructor does not retain the caller's mutable list"() {
        given:
        def inputTags = ["rent"]

        when:
        def draft = new TransferDraft("PLN-1", "PLN-2", BigDecimal.TEN, inputTags)
        inputTags << "external-change"

        then:
        draft.tags() == ["rent"]
    }

    def "tags accessor does not expose the internal mutable list"() {
        given:
        def draft = new TransferDraft("PLN-1", "PLN-2", BigDecimal.TEN, ["rent"])

        when:
        draft.tags().add("external-change")

        then:
        thrown(UnsupportedOperationException)
        draft.tags() == ["rent"]
    }

    @Unroll
    def "constructor rejects invalid draft: #description"() {
        when:
        new TransferDraft(from, to, amount, tags)

        then:
        thrown(expected)

        where:
        description       | from    | to      | amount                  | tags || expected
        "blank source"    | " "     | "PLN-2" | BigDecimal.ONE          | []   || IllegalArgumentException
        "same accounts"   | "PLN-1" | "PLN-1" | BigDecimal.ONE          | []   || IllegalArgumentException
        "zero amount"     | "PLN-1" | "PLN-2" | BigDecimal.ZERO         | []   || IllegalArgumentException
        "negative amount" | "PLN-1" | "PLN-2" | new BigDecimal("-1.00") | []   || IllegalArgumentException
        "null tags"       | "PLN-1" | "PLN-2" | BigDecimal.ONE          | null || NullPointerException
    }
}
```

## Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.patterns.prototype.PrototypeSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.patterns.prototype.PrototypeSpec"
```

## Eksperymenty

1. Dodaj do prototypu mapę metadanych. Skopiuj ją płytko, a potem głęboko.
   Który test wykryje współdzielenie mapy?
2. Napisz `TransferDraftRegistry`, który przechowuje nazwane prototypy
   i zwraca ich kopie. Zadbaj, by klient nie mógł zmienić prototypu
   przechowywanego w rejestrze.
3. Zastanów się, jak skopiować pole zawierające obiekt własnej klasy.
   Czy wystarczy skopiować referencję, czy potrzebna jest jego kopia?
4. Porównaj `copy()` z builderem z Laba 39. Które podejście lepiej
   pasuje do tworzenia podobnych obiektów, a które do konstruowania
   nowego obiektu z wielu parametrów?
5. Zastąp jawne kopiowanie przez `Cloneable` i `Object.clone()`.
   Zapisz, jakie problemy rozwiązują konstruktor kopiujący i typowane
   `copy()`.

## Jak odpowiedzieć na rozmowie

**Prototype** tworzy nowe obiekty przez kopiowanie skonfigurowanego
prototypu. Bywa użyteczny, gdy konfiguracja jest złożona lub obiekty
powstają w wielu podobnych wariantach. Rejestr prototypów może wybierać
kopię po kluczu.

Kluczowe pytanie dotyczy semantyki kopii: **płytka** kopia współdzieli
referencje do pól, a **głęboka** kopiuje także mutowalne obiekty
wewnętrzne. Nie każda kopia musi być głęboka — trzeba ustalić, czy
współdzielenie danego stanu jest bezpieczne.

W Javie jawny konstruktor kopiujący lub typowana metoda `copy()` często
są prostsze od `Cloneable`/`Object.clone()`: jasno pokazują, które pola
są kopiowane i jak. Wada Prototype to dodatkowa odpowiedzialność za
poprawne kopiowanie oraz trudność przy cyklicznych grafach obiektów.

## Pytania kontrolne

1. Czym różni się kopia płytka od głębokiej?
2. Dlaczego kopia z tego laba ma nową listę, ale może współdzielić `String` i `BigDecimal`?
3. Kiedy rejestr prototypów może uprościć tworzenie obiektów?
4. Jak Prototype różni się od Buildera i Factory?
5. Dlaczego `Object.clone()` bywa trudniejsze w utrzymaniu niż jawny kontrakt `copy()`?

## Do zapamiętania

- Prototype tworzy nowy obiekt na podstawie istniejącego egzemplarza.
- Zdecyduj jawnie, które elementy kopii są współdzielone, a które niezależne.
- Jawne `copy()` lub konstruktor kopiujący ujawniają semantykę lepiej niż nieprzemyślana kopia płytka.
