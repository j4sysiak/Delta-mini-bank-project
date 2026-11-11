# Lab 33 — Groovy: wyrażenia regularne i dopasowywanie tekstu

## Dlaczego ten temat?

Wyrażenia regularne przydają się do sprawdzania formatu danych i
wyszukiwania wzorców w tekście. W Groovy składnia operatorów
`==~` i `=~` sprawia, że można używać regexów bezpośrednio
w wyrażeniach.

W tym labie przećwiczysz:

- dopasowanie całego tekstu przez `==~`,
- wyszukiwanie fragmentów przez `=~` i `Matcher.find()`,
- odczyt grup nazwanych,
- poprawne parsowanie kwot do `BigDecimal`,
- różnicę między walidacją formatu a walidacją reguły biznesowej.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. Regex w Groovy

Regex może być zapisany jako zwykły tekst:

```groovy
def regex = "\\d{4}"
```

Wymaga to podwójnego zapisu ukośnika, bo backslash jest też znakiem
specjalnym w zwykłym stringu. Groovy oferuje **slashy string**, który
zwykle jest wygodniejszy dla regexów:

```groovy
def regex = /\d{4}/
```

W slashy string backslash nie wymaga dodatkowego escapowania. Sam znak
ukośnika `/` trzeba natomiast zapisać jako `\/`.

## 2. `==~`: dopasuj cały tekst

Operator `==~` sprawdza, czy cały tekst pasuje do regexu:

```groovy
"PLN-1001" ==~ /[A-Z]{3}-\d{4}/ // true
"Account PLN-1001" ==~ /[A-Z]{3}-\d{4}/ // false
```

To odpowiednik walidacji całego wejścia. Nie trzeba dodawać `^` i `$`
wyłącznie po to, aby wymusić dopasowanie od początku do końca.

Wynikiem `==~` jest `boolean`, nie obiekt `Matcher`.

## 3. `=~`: znajdź dopasowania w tekście

Operator `=~` tworzy `Matcher`:

```groovy
def matcher = "Transfer PLN-1001 -> EUR-2002" =~ /[A-Z]{3}-\d{4}/

matcher.find() // true
matcher.group() // PLN-1001
matcher.find() // true
matcher.group() // EUR-2002
matcher.find() // false
```

`find()` szuka kolejnego dopasowania w tekście. To inne zachowanie niż
`==~`, które wymaga dopasowania całego wejścia.

Wywołanie `group()` jest poprawne po udanym `find()`. Bez wcześniejszego
dopasowania `Matcher.group()` zgłasza `IllegalStateException`.

## 4. Grupy nazwane

Nawiasy w regexie tworzą grupę przechwytującą. Nazwana grupa ma postać:

```regex
(?<from>[A-Z]{3}-\d{4})
```

Po udanym `find()` odczytasz ją przez:

```groovy
matcher.group("from")
```

Nazwane grupy ułatwiają czytanie parsera: `from`, `to` i `amount` są
czytelniejsze niż indeksy `group(1)`, `group(2)` i `group(3)`.

## 5. Format transferu w ćwiczeniu

Parser będzie rozpoznawał wpisy w formacie:

```text
TRANSFER PLN-1001 -> EUR-2002; amount=125.50
```

Założenia:

- numer konta ma trzy wielkie litery, myślnik i cztery cyfry,
- kwota jest nieujemnym tekstem z maksymalnie dwoma cyframi po kropce,
- parser wyszukuje wpisy wewnątrz większego tekstu,
- tekst poprawny składniowo nie musi oznaczać poprawnej operacji bankowej.

Regex może sprawdzić strukturę tekstu. Nie zastępuje walidacji, czy konta
istnieją, czy mają tę samą walutę albo czy saldo jest wystarczające.

## 6. Model wyniku

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\regex
```

Utwórz plik `TransferRoute.groovy`:

```groovy
package com.delta.bank.lab.groovy.regex

import java.math.BigDecimal

class TransferRoute {
    final String fromAccount
    final String toAccount
    final BigDecimal amount

    TransferRoute(String fromAccount, String toAccount, BigDecimal amount) {
        this.fromAccount = fromAccount
        this.toAccount = toAccount
        this.amount = amount
    }
}
```

## 7. Scanner i parser

Utwórz plik `TransferTextParser.groovy` w tym samym katalogu:

```groovy
package com.delta.bank.lab.groovy.regex

import java.math.BigDecimal
import java.util.Objects

class TransferTextParser {

    private static final String ACCOUNT_NUMBER = /[A-Z]{3}-\d{4}/

    private static final String TRANSFER_PATTERN =
            /TRANSFER\s+(?<from>${ACCOUNT_NUMBER})\s*->\s*(?<to>${ACCOUNT_NUMBER})\s*;\s*amount=(?<amount>\d+(?:\.\d{1,2})?)(?![\d.])/

    boolean isAccountNumber(String value) {
        return value != null && value ==~ /[A-Z]{3}-\d{4}/
    }

    List<String> findAccountNumbers(String text) {
        Objects.requireNonNull(text, "text must not be null")

        def matcher = text =~ /(?<![A-Z0-9-])[A-Z]{3}-\d{4}(?![A-Z0-9-])/
        List<String> accountNumbers = []

        while (matcher.find()) {
            accountNumbers.add(matcher.group())
        }

        return List.copyOf(accountNumbers)
    }

    List<TransferRoute> parseTransfers(String text) {
        Objects.requireNonNull(text, "text must not be null")

        def matcher = text =~ TRANSFER_PATTERN
        List<TransferRoute> transfers = []

        while (matcher.find()) {
            transfers.add(new TransferRoute(
                    matcher.group("from"),
                    matcher.group("to"),
                    new BigDecimal(matcher.group("amount"))
            ))
        }

        return List.copyOf(transfers)
    }
}
```

Zwróć uwagę:

- `==~` w `isAccountNumber` waliduje cały numer konta.
- `=~` zwraca matcher, który można przeszukiwać przez `find()`.
- `findAccountNumbers` nie uznaje numeru będącego częścią dłuższego
  tokenu, dzięki lookbehind i lookahead.
- `TRANSFER_PATTERN` używa interpolacji slashy string, aby użyć wspólnego
  fragmentu regexu `ACCOUNT_NUMBER`.
- kwota jest konwertowana z tekstu do `BigDecimal`, a nie do `double`.
- `(?![\d.])` zapobiega zaakceptowaniu początku kwoty z nadmiarowymi
  cyframi lub kolejną kropką, np. `12.345`.

Regex z lookbehind/lookahead działa tu na zwykłym tekście. Jeśli będziesz
używać bardziej skomplikowanych wyrażeń regularnych albo danych
użytkownika jako fragmentu regexu, poznaj ograniczenia używanego silnika
regexów i nie wstawiaj niezaufanych fragmentów bez `Pattern.quote(...)`.

## 8. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\regex
```

Utwórz w nim `TransferTextParserSpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.regex

import spock.lang.Specification
import spock.lang.Unroll

import java.math.BigDecimal

class TransferTextParserSpec extends Specification {

    private final TransferTextParser parser = new TransferTextParser()

    @Unroll
    def "==~ validates the entire account number: #value"() {
        expect:
        parser.isAccountNumber(value) == valid

        where:
        value            | valid
        "PLN-1001"       | true
        "EUR-0007"       | true
        "Account PLN-1001" | false
        "pln-1001"       | false
        "PLN-100"        | false
        "PLN-10010"      | false
        ""               | false
        null             | false
    }

    def "=~ finds account numbers inside a larger text"() {
        expect:
        parser.findAccountNumbers(
                "Transfer PLN-1001 -> EUR-2002; invalid XPLN-3003 and PLN-40000"
        ) == ["PLN-1001", "EUR-2002"]
    }

    def "returns an empty list when there are no account numbers"() {
        expect:
        parser.findAccountNumbers("No account references here") == []
    }

    def "parses named groups into transfer routes"() {
        when:
        def routes = parser.parseTransfers(
                "Audit: TRANSFER PLN-1001 -> EUR-2002; amount=125.50"
        )

        then:
        routes.size() == 1
        routes[0].fromAccount == "PLN-1001"
        routes[0].toAccount == "EUR-2002"
        routes[0].amount.compareTo(new BigDecimal("125.50")) == 0
    }

    def "finds multiple transfers in one text"() {
        when:
        def routes = parser.parseTransfers(
                "TRANSFER PLN-1001 -> EUR-2002; amount=10.00\n" +
                "TRANSFER EUR-2002 -> USD-3003; amount=7"
        )

        then:
        routes*.amount == [
                new BigDecimal("10.00"),
                new BigDecimal("7")
        ]
    }

    @Unroll
    def "does not parse malformed transfer amount: #amount"() {
        expect:
        parser.parseTransfers(
                "TRANSFER PLN-1001 -> EUR-2002; amount=${amount}"
        ).isEmpty()

        where:
        amount << ["", "-1.00", "1.234", "1."]
    }

    def "rejects null input text"() {
        when:
        parser.findAccountNumbers(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "text must not be null"
    }
}
```

Test z `==~` sprawdza cały tekst, a test z `=~` wyszukuje dopasowania
w środku tekstu. Zwróć uwagę, że format kwoty dopuszcza `7`, `7.5`
i `7.50`, ale nie kwoty ujemnej ani trzech cyfr po kropce.

## 9. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.regex.TransferTextParserSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.regex.TransferTextParserSpec"
```

Test nie wymaga uruchomienia kontenera PostgreSQL.

## 10. Eksperymenty

### Eksperyment A: zamień `==~` na `=~`

Zmień tymczasowo implementację `isAccountNumber` na:

```groovy
return value != null && value =~ /[A-Z]{3}-\d{4}/
```

Sprawdź typ wyniku i test dla `"Account PLN-1001"`. Operator `=~`
zwraca `Matcher`, a w warunku może zostać poddany konwersji do boolean
przez sprawdzenie, czy istnieje dopasowanie. Dlatego nie zastępuje
walidacji pełnego wejścia.

Przywróć `==~`.

### Eksperyment B: usuń lookaround

W `findAccountNumbers` użyj prostego regexu:

```groovy
/[A-Z]{3}-\d{4}/
```

Uruchom test. Zobacz, dlaczego dopasowanie może zostać wycięte z dłuższego
tokenu, np. `XPLN-3003` albo `PLN-40000`. Przywróć lookbehind i lookahead.

### Eksperyment C: zmień separator dziesiętny

Dodaj obsługę kwoty z przecinkiem, np. `125,50`. Zdecyduj, czy parser ma
normalizować przecinek do kropki przed utworzeniem `BigDecimal`, czy
odrzucać taki format. Dopisz test określający wybrany kontrakt.

## 11. Regex a walidacja domenowa

Wyrażenie regularne potrafi zweryfikować składnię, np. liczbę znaków
i układ numeru. Nie sprawdza jednak:

- czy konto istnieje,
- czy właściciel może wykonać transfer,
- czy konta mają tę samą walutę,
- czy saldo jest wystarczające,
- czy kwota mieści się w regułach biznesowych.

Traktuj regex jako jedną warstwę sprawdzenia tekstu, a nie zamiennik
walidacji domenowej.

## Pytania kontrolne

1. Jaka jest różnica między `==~` a `=~`?
2. Co zwraca operator `=~`?
3. Kiedy można bezpiecznie wywołać `matcher.group()`?
4. Jak odczytać nazwaną grupę `from`?
5. Dlaczego parser konwertuje kwotę bezpośrednio do `BigDecimal`?
6. Co robi lookahead `(?![\d.])` w regexie kwoty?
7. Czy poprawny składniowo numer konta oznacza, że konto istnieje?
8. Kiedy należy użyć `Pattern.quote(...)`?

Do zapamiętania:

```text
==~  → dopasowanie całego tekstu, wynik boolean
=~   → Matcher do szukania dopasowań
find() → szuka kolejnego dopasowania
group("name") → odczytuje nazwaną grupę po udanym find()
regex → sprawdza strukturę tekstu, nie reguły domenowe
kwoty → parsuj do BigDecimal, nie double
```
