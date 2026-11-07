# Lab 26 — Groovy: bezpieczne odwołania, Elvis i Groovy Truth

## Dlaczego ten temat?

Lab 24 pokazał `Optional` w Javie, a Lab 25 wprowadził closures i metody
kolekcji Groovy. Teraz przećwiczysz idiomatyczne operatory Groovy do pracy
z brakującymi danymi:

- safe navigation: `?.`,
- Elvis: `?:`,
- Groovy Truth, czyli konwersję wartości na `boolean`.

Zobaczysz też ważną pułapkę: Elvis traktuje jako „brak” nie tylko `null`,
ale również inne wartości fałszywe w Groovy, na przykład `0` i pusty tekst.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. Safe navigation: `?.`

Zwykły łańcuch wywołań:

```groovy
profile.contact.email
```

rzuci `NullPointerException`, jeśli `profile` albo `contact` ma wartość
`null`.

Safe navigation zwraca `null`, gdy odbiorca konkretnego `?.` jest `null`:

```groovy
profile?.contact?.email
```

Operator trzeba stosować na kolejnych elementach łańcucha, które mogą
być `null`. `?.` nie oznacza „ignoruj każdy błąd” — na przykład nie
przechwytuje wyjątków rzucanych przez metodę, która została wywołana.

## 2. Elvis: `?:`

Operator Elvis jest krótszą formą wyboru wartości domyślnej:

```groovy
def label = value ?: "brak"
```

Jeśli `value` jest prawdziwe w Groovy, wynikiem jest `value`; w przeciwnym
razie wynikiem jest tekst domyślny.

Często łączy się go z safe navigation:

```groovy
def email = profile?.contact?.email ?: "brak adresu"
```

Można to czytać tak:

1. spróbuj odczytać zagnieżdżony adres,
2. jeśli wynik jest „fałszywy” według Groovy Truth, wybierz wartość domyślną.

## 3. Groovy Truth

Groovy pozwala używać wartości w warunku, nawet jeśli ich typem nie jest
`boolean`. Typowe wartości fałszywe to:

| Wartość | W Groovy Truth |
|---|---|
| `null` | fałsz |
| `false` | fałsz |
| liczba równa zero | fałsz |
| pusty tekst `""` | fałsz |
| pusta kolekcja `[]` | fałsz |
| pusta mapa `[:]` | fałsz |
| niepusty tekst, także `"false"` i `"0"` | prawda |
| niepusta kolekcja | prawda |

Własne typy mogą określić tę konwersję przez metodę `asBoolean()`.

Groovy Truth jest wygodna w warunkach:

```groovy
if (profile?.tags) {
    // lista istnieje i zawiera elementy
}
```

Ale przez to Elvis nie zawsze jest zamiennikiem kontroli `null`.

## 4. Pułapka: Elvis a zero

Załóżmy, że zero jest poprawnym limitem, a domyślny limit wynosi 100:

```groovy
def limit = requestedLimit ?: defaultLimit
```

Jeśli `requestedLimit` wynosi `BigDecimal.ZERO`, Groovy uzna je za fałszywe
i zwróci `defaultLimit`. To zmienia poprawne zero na 100.

Jeśli domyślną wartość wybierasz wyłącznie dla `null`, użyj jawnego
sprawdzenia:

```groovy
def limit = requestedLimit != null ? requestedLimit : defaultLimit
```

Używaj `?:` wtedy, gdy chcesz zastąpić wszystkie wartości fałszywe według
Groovy Truth. Gdy znaczenie ma wyłącznie brak wartości, sprawdzaj `null`
wprost.

## 5. Model szkoleniowy

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\nullsafety
```

Utwórz plik `AccountContact.groovy`:

```groovy
package com.delta.bank.lab.groovy.nullsafety

class AccountContact {
    final String email

    AccountContact(String email) {
        this.email = email
    }
}
```

Utwórz plik `AccountProfile.groovy`:

```groovy
package com.delta.bank.lab.groovy.nullsafety

class AccountProfile {
    final String owner
    final AccountContact contact
    final List<String> tags

    AccountProfile(String owner, AccountContact contact, List<String> tags) {
        this.owner = owner
        this.contact = contact
        this.tags = tags
    }
}
```

Te proste klasy celowo dopuszczają `null`. W prawdziwym modelu decyzja,
czy pole może być puste, powinna wynikać z kontraktu danych.

## 6. Formatter

Utwórz plik `AccountProfileFormatter.groovy` w tym samym katalogu:

```groovy
package com.delta.bank.lab.groovy.nullsafety

import java.math.BigDecimal

class AccountProfileFormatter {

    String ownerLabel(AccountProfile profile) {
        profile?.owner?.strip() ?: "Unknown owner"
    }

    String emailLabel(AccountProfile profile) {
        profile?.contact?.email ?: "No email"
    }

    boolean hasTags(AccountProfile profile) {
        profile?.tags ? true : false
    }

    BigDecimal effectiveLimit(BigDecimal requestedLimit, BigDecimal defaultLimit) {
        requestedLimit != null ? requestedLimit : defaultLimit
    }
}
```

Przyjrzyj się różnym regułom:

- `ownerLabel` używa `?.` dla obiektu i pola, a następnie `?:`.
- `emailLabel` bezpiecznie przechodzi przez dwa możliwe braki.
- `hasTags` korzysta z Groovy Truth: `null` i pusta lista dają `false`.
- `effectiveLimit` sprawdza tylko `null`, żeby zachować poprawne zero.

W `ownerLabel` pusty lub składający się z białych znaków tekst po `strip()`
jest zastąpiony wartością domyślną. W `emailLabel` pusty tekst również
wybiera wartość domyślną. To są świadome reguły tego ćwiczenia.

## 7. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\nullsafety
```

Utwórz w nim `AccountProfileFormatterSpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.nullsafety

import spock.lang.Specification
import spock.lang.Unroll

import java.math.BigDecimal

class AccountProfileFormatterSpec extends Specification {

    private final AccountProfileFormatter formatter =
            new AccountProfileFormatter()

    def "safe navigation reads a nested email"() {
        given:
        def profile = new AccountProfile(
                "Anna Kowalska",
                new AccountContact("anna@example.test"),
                ["premium"]
        )

        expect:
        formatter.emailLabel(profile) == "anna@example.test"
    }

    @Unroll
    def "Elvis supplies an email label for #caseName"() {
        expect:
        formatter.emailLabel(profile) == "No email"

        where:
        caseName             | profile
        "a null profile"     | null
        "a null contact"     | new AccountProfile("Anna", null, [])
        "a null email"       | new AccountProfile("Anna", new AccountContact(null), [])
        "an empty email"     | new AccountProfile("Anna", new AccountContact(""), [])
    }

    def "owner label strips whitespace before applying its fallback"() {
        expect:
        formatter.ownerLabel(new AccountProfile("  Anna Kowalska  ", null, [])) ==
                "Anna Kowalska"
        formatter.ownerLabel(new AccountProfile("  ", null, [])) ==
                "Unknown owner"
        formatter.ownerLabel(null) == "Unknown owner"
    }

    @Unroll
    def "Groovy Truth determines whether profile has tags: #caseName"() {
        expect:
        formatter.hasTags(profile) == expected

        where:
        caseName          | profile                                      | expected
        "null profile"    | null                                         | false
        "null tags"       | new AccountProfile("Anna", null, null)       | false
        "empty tags"      | new AccountProfile("Anna", null, [])         | false
        "one tag"         | new AccountProfile("Anna", null, ["premium"]) | true
    }

    def "explicit null check preserves zero as a valid limit"() {
        expect:
        formatter.effectiveLimit(
                BigDecimal.ZERO,
                new BigDecimal("100.00")
        ).compareTo(BigDecimal.ZERO) == 0
    }

    def "explicit null check uses the default only for null"() {
        expect:
        formatter.effectiveLimit(
                null,
                new BigDecimal("100.00")
        ).compareTo(new BigDecimal("100.00")) == 0
    }

    def "Groovy Truth treats empty values as false but nonempty strings as true"() {
        expect:
        truthy(null) == false
        truthy(false) == false
        truthy(0) == false
        truthy(BigDecimal.ZERO) == false
        truthy("") == false
        truthy([]) == false
        truthy([:]) == false

        truthy("false") == true
        truthy("0") == true
        truthy(["premium"]) == true
    }

    private static boolean truthy(Object value) {
        value ? true : false
    }
}
```

Metoda `truthy` pozwala zobaczyć konwersję wartości na `boolean` wprost.
Zwróć uwagę, że niepusty tekst `"false"` jest w Groovy prawdziwy — Groovy
sprawdza, czy tekst jest pusty, a nie czy zawiera słowo `"true"`.

## 8. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.nullsafety.AccountProfileFormatterSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.nullsafety.AccountProfileFormatterSpec"
```

Test nie wymaga uruchomienia kontenera PostgreSQL.

## 9. Eksperymenty

### Eksperyment A: usuń safe navigation

Zmień tymczasowo:

```groovy
profile?.contact?.email
```

na:

```groovy
profile.contact.email
```

Uruchom spec. Sprawdź, który przypadek przestaje działać i dlaczego.
Przywróć safe navigation.

### Eksperyment B: zamień jawny null check na Elvis

Zmień tymczasowo `effectiveLimit` na:

```groovy
requestedLimit ?: defaultLimit
```

Test z `BigDecimal.ZERO` powinien ujawnić błąd reguły. Przywróć wersję,
która wybiera wartość domyślną tylko wtedy, gdy argument jest `null`.

### Eksperyment C: sprawdź pusty tekst

W `emailLabel` porównaj wynik dla:

```groovy
new AccountContact(null)
new AccountContact("")
new AccountContact(" ")
```

Operator Elvis zastępuje `null` i `""`, ale zwykły tekst zawierający
spację jest niepusty, więc jest prawdziwy. Jeśli spacje też mają oznaczać
brak adresu, jawnie oczyść tekst przed użyciem Elvis:

```groovy
profile?.contact?.email?.strip() ?: "No email"
```

## 10. Porównanie z Javą i `Optional`

Safe navigation i Elvis są operatorami Groovy; nie zmieniają kontraktu
Javy ani nie dodają walidacji danych. W Javie analogiczny przepływ można
zapisać na przykład przez `Optional.map(...).orElse(...)`, ale warto
pamiętać o różnicy:

- Elvis wybiera domyślną wartość dla wszystkich wartości fałszywych według
  Groovy Truth,
- `Optional.orElse(...)` wybiera wartość domyślną tylko wtedy, gdy
  `Optional` jest pusty,
- jawny warunek `value != null` również rozróżnia `null` od zera, `false`
  i pustego tekstu.

## Pytania kontrolne

1. Co zwraca `profile?.contact`, gdy `profile` jest `null`?
2. Dlaczego każdy możliwie pusty element łańcucha wymaga własnego `?.`?
3. Czy Elvis sprawdza wyłącznie `null`?
4. Dlaczego `BigDecimal.ZERO ?: defaultLimit` może być błędne?
5. Czy tekst `"false"` jest fałszywy w Groovy Truth?
6. Jaka jest różnica między pustą listą a listą zawierającą jeden element?
7. Kiedy należy użyć jawnego warunku `value != null`?
8. Czy safe navigation przechwytuje wyjątki rzucane przez wywołaną metodę?

Do zapamiętania:

```text
?.       → bezpiecznie wywołaj metodę/odczytaj właściwość, gdy odbiorca może być null
?:       → wybierz wartość domyślną dla wartości false według Groovy Truth
Groovy Truth → null, false, zero i puste kolekcje/teksty są fałszywe
value != null ? value : fallback → fallback tylko dla null
```
