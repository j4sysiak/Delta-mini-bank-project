# Lab 34 — Groovy: JSON odczyt i zapis przez `JsonSlurper` i `JsonOutput`

## Dlaczego ten temat?

JSON jest częstym formatem wymiany danych między API. Groovy udostępnia
proste narzędzia do parsowania JSON-u i generowania go z map oraz list.

W tym labie przećwiczysz:

- parsowanie JSON-u przez `JsonSlurper`,
- bezpieczny odczyt zagnieżdżonych obiektów i tablic,
- rozróżnianie typów po parsowaniu,
- generowanie JSON-u przez `JsonOutput`,
- walidację danych wejściowych przed użyciem.

Projekt zawiera `localGroovy()` w Gradle, więc używamy dostępnego Groovy
JSON API i nie dodajemy zależności. Nie dodawaj ani nie zmieniaj
konfiguracji Gradle.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego
ani istniejących labów. Ćwiczenie działa bez Springa, bazy danych,
sieci i preview Java.

## 1. JSON i typy po parsowaniu

Przykładowy dokument:

```json
{
  "accountNumber": "PLN-1001",
  "currency": "PLN",
  "balance": 125.50,
  "active": true,
  "tags": ["personal", "premium"]
}
```

`JsonSlurper` parsuje dokument JSON do struktur Groovy/Java:

- obiekt JSON → zwykle mapa,
- tablica JSON → lista,
- string → `String`,
- boolean → `Boolean`,
- `null` → `null`,
- liczba → typ liczbowy, który należy jawnie sprawdzić przed konwersją.

Nie zakładaj, że każda liczba z JSON-u od razu jest `BigDecimal`.
Zachowanie zależy od parsera i konfiguracji. W tym labie sprawdzimy
dozwolone typy i przekształcimy kwotę do `BigDecimal` przez tekstową
reprezentację, unikając `double`.

## 2. Parsowanie JSON-u

Najprostszy przykład:

```groovy
import groovy.json.JsonSlurper

def parsed = new JsonSlurper().parseText('{"currency":"PLN"}')
assert parsed.currency == "PLN"
```

Dla JSON-u pochodzącego od użytkownika lub z sieci nie wystarczy sam
parser. Sprawdź strukturę, wymagane pola, typy i reguły biznesowe.
Parser potwierdza, że tekst jest poprawnym JSON-em — nie potwierdza,
że jest poprawnym żądaniem bankowym.

## 3. Model transferu

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\json
```

Utwórz plik `TransferCommand.groovy`:

```groovy
package com.delta.bank.lab.groovy.json

import java.math.BigDecimal

class TransferCommand {
    final String requestId
    final String fromAccount
    final String toAccount
    final BigDecimal amount

    TransferCommand(
            String requestId,
            String fromAccount,
            String toAccount,
            BigDecimal amount
    ) {
        this.requestId = requestId
        this.fromAccount = fromAccount
        this.toAccount = toAccount
        this.amount = amount
    }
}
```

## 4. Parser z walidacją

Utwórz plik `TransferCommandParser.groovy` w tym samym katalogu:

```groovy
package com.delta.bank.lab.groovy.json

import groovy.json.JsonSlurper

import java.math.BigDecimal
import java.util.Objects

class TransferCommandParser {

    private static final Set<String> ALLOWED_KEYS = [
            "requestId",
            "fromAccount",
            "toAccount",
            "amount"
    ] as Set<String>

    TransferCommand parse(String json) {
        Objects.requireNonNull(json, "json must not be null")

        def parsed = new JsonSlurper().parseText(json)
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("JSON root must be an object")
        }

        Map document = (Map) parsed
        def unknownKeys = document.keySet()
                .findAll { key -> !(key instanceof String) || !ALLOWED_KEYS.contains(key) }

        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown field(s): ${unknownKeys.collect { it.toString() }.sort().join(', ')}"
            )
        }

        String requestId = requiredText(document, "requestId")
        String fromAccount = requiredText(document, "fromAccount")
        String toAccount = requiredText(document, "toAccount")
        BigDecimal amount = requiredAmount(document, "amount")

        if (fromAccount == toAccount) {
            throw new IllegalArgumentException(
                    "fromAccount and toAccount must be different"
            )
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero")
        }

        return new TransferCommand(
                requestId,
                fromAccount,
                toAccount,
                amount
        )
    }

    private static String requiredText(Map document, String field) {
        if (!document.containsKey(field)) {
            throw new IllegalArgumentException("${field} is required")
        }

        def value = document[field]
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("${field} must be a String")
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("${field} must not be blank")
        }

        return value
    }

    private static BigDecimal requiredAmount(Map document, String field) {
        if (!document.containsKey(field)) {
            throw new IllegalArgumentException("${field} is required")
        }

        def value = document[field]
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("${field} must be a JSON number")
        }

        try {
            return new BigDecimal(value.toString())
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "${field} must be a valid decimal number",
                    exception
            )
        }
    }
}
```

Parser odrzuca zagnieżdżone obiekty i nieznane pola, bo schemat tego
żądania płaski i jawny. W prawdziwym API reguły ignorowania albo
odrzucania nieznanych pól powinny wynikać z ustalonego kontraktu.

Kwota musi być JSON-ową liczbą, nie tekstem. Konwersja przez
`value.toString()` do `BigDecimal` pozwala uniknąć obliczeń na
`double`, ale nie zastępuje walidacji zakresu ani skali wymaganej
przez domenę.

## 5. Generowanie JSON-u

Utwórz plik `TransferCommandJson.groovy` w tym samym katalogu:

```groovy
package com.delta.bank.lab.groovy.json

import groovy.json.JsonOutput

class TransferCommandJson {

    String write(TransferCommand command) {
        if (command == null) {
            throw new NullPointerException("command must not be null")
        }

        Map document = [
                requestId : command.requestId,
                fromAccount: command.fromAccount,
                toAccount  : command.toAccount,
                amount     : command.amount
        ]

        return JsonOutput.toJson(document)
    }
}
```

`JsonOutput.toJson(...)` wykonuje escapowanie tekstu zgodnie z JSON-em.
Nie buduj JSON-u przez ręczne łączenie stringów.

Wynikowy JSON nie musi zachować formatowania wejściowego, białych znaków
ani kolejności pól. Sprawdzaj strukturę danych po ponownym parsowaniu,
nie porównuj całego tekstu, chyba że format tekstowy jest częścią
kontraktu.

## 6. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\json
```

Utwórz w nim `TransferCommandJsonSpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.json

import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Unroll

import java.math.BigDecimal

class TransferCommandJsonSpec extends Specification {

    private final TransferCommandParser parser = new TransferCommandParser()
    private final TransferCommandJson writer = new TransferCommandJson()

    def "parses a valid transfer command"() {
        when:
        def command = parser.parse(validJson())

        then:
        command.requestId == "req-1001"
        command.fromAccount == "PLN-1001"
        command.toAccount == "PLN-2002"
        command.amount.compareTo(new BigDecimal("125.50")) == 0
    }

    def "writes JSON that can be parsed back to the same values"() {
        given:
        def command = parser.parse(validJson())

        when:
        def output = writer.write(command)
        def parsedAgain = new JsonSlurper().parseText(output)

        then:
        parsedAgain.requestId == "req-1001"
        parsedAgain.fromAccount == "PLN-1001"
        parsedAgain.toAccount == "PLN-2002"
        new BigDecimal(parsedAgain.amount.toString())
                .compareTo(new BigDecimal("125.50")) == 0
    }

    @Unroll
    def "rejects JSON with an invalid root or required field: #caseName"() {
        when:
        parser.parse(json)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == expectedMessage

        where:
        caseName             | json                                                                    | expectedMessage
        "array root"         | "[]"                                                                    | "JSON root must be an object"
        "missing request id" | '{"fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":1}'          | "requestId is required"
        "null request id"    | '{"requestId":null,"fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":1}' | "requestId must be a String"
        "blank request id"   | '{"requestId":" ","fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":1}' | "requestId must not be blank"
        "string amount"      | '{"requestId":"r1","fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":"1.00"}' | "amount must be a JSON number"
        "unknown field"      | '{"requestId":"r1","fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":1,"memo":"x"}' | "Unknown field(s): memo"
    }

    def "rejects a non-positive amount"() {
        when:
        parser.parse('{"requestId":"r1","fromAccount":"PLN-1001","toAccount":"PLN-2002","amount":0}')

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "amount must be greater than zero"
    }

    def "rejects identical source and destination accounts"() {
        when:
        parser.parse('{"requestId":"r1","fromAccount":"PLN-1001","toAccount":"PLN-1001","amount":1}')

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "fromAccount and toAccount must be different"
    }

    def "rejects null JSON text"() {
        when:
        parser.parse(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "json must not be null"
    }

    def "rejects null command when writing JSON"() {
        when:
        writer.write(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "command must not be null"
    }

    private static String validJson() {
        '''
        {
          "requestId": "req-1001",
          "fromAccount": "PLN-1001",
          "toAccount": "PLN-2002",
          "amount": 125.50
        }
        '''
    }
}
```

Wartości w przykładach JSON są rzeczywistym JSON-em. Pamiętaj, że tekst
`"125.50"` jest stringiem, a `125.50` jest liczbą — parser rozróżnia
te typy.

## 7. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.json.TransferCommandJsonSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.json.TransferCommandJsonSpec"
```

Test nie wymaga uruchomienia kontenera PostgreSQL.

## 8. Eksperymenty

### Eksperyment A: zmień liczbę na tekst

W JSON-ie zmień:

```json
"amount": 125.50
```

na:

```json
"amount": "125.50"
```

Uruchom test. Zwróć uwagę, że składniowo poprawny JSON nadal może mieć
niepoprawny typ danych dla kontraktu parsera.

### Eksperyment B: usuń walidację nieznanych pól

Usuń sprawdzanie `unknownKeys` i uruchom spec. Test pola `memo` przestanie
przechodzić. Zastanów się, czy API powinno odrzucać nieznane pola, czy
ignorować je dla zgodności wstecznej. To decyzja kontraktowa.

### Eksperyment C: sprawdź escapowanie

Utwórz tekstowe pole zawierające cudzysłów lub backslash, zapisz obiekt
przez `JsonOutput`, a następnie sparsuj ponownie. Potwierdź, że parser
odzyskuje oryginalną wartość.

### Eksperyment D: zbadaj typ liczby

W teście wypisz:

```groovy
parsedAgain.amount.getClass()
```

Nie zakładaj konkretnego typu implementacyjnego bez sprawdzenia.
Kontrakt ćwiczenia opiera się na wartości liczbowej, a kwotę domenową
konwertujemy jawnie do `BigDecimal`.

## 9. Dobre praktyki

- Parsowanie JSON-u i walidacja żądania to dwa osobne kroki.
- Sprawdzaj typy i wymagane pola; nie polegaj na automatycznej konwersji
  Groovy.
- Odróżniaj JSON-ową liczbę od JSON-owego stringa zawierającego cyfry.
- Nie używaj `double` do kwot.
- Generuj JSON przez `JsonOutput`, nie ręcznym sklejaniem tekstu.
- Nie loguj nieprzefiltrowanych payloadów, jeśli mogą zawierać dane
  wrażliwe.
- Ustal jawny kontrakt dla nieznanych pól i wartości `null`.

## Pytania kontrolne

1. Jakie typy Groovy/Java zwraca `JsonSlurper` dla obiektu, tablicy,
   stringa, boolean i `null`?
2. Czy poprawny składniowo JSON oznacza poprawne żądanie transferu?
3. Jaka jest różnica między `125.50` a `"125.50"` w JSON-ie?
4. Dlaczego parser konwertuje kwotę do `BigDecimal` przez tekstową
   reprezentację?
5. Czy `JsonOutput.toJson(...)` gwarantuje zachowanie formatowania
   wejściowego?
6. Dlaczego test round-trip ponownie parsuje JSON zamiast porównywać
   cały tekst?
7. Kiedy nieznane pola należy odrzucić, a kiedy można je ignorować?

Do zapamiętania:

```text
JsonSlurper → parsuje tekst JSON do map, list i wartości prostych
JsonOutput → generuje poprawnie escapowany JSON
poprawny JSON ≠ poprawne żądanie domenowe
liczba JSON ≠ string JSON
kwota → sprawdź typ i konwertuj do BigDecimal, nie double
```
