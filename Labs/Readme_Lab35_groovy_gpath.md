# Lab 35 — Groovy: GPath i nawigacja po zagnieżdżonych danych

## Dlaczego ten temat?

Lab 34 wprowadził parsowanie JSON-u do map i list. Teraz przećwiczysz
czytelne odczytywanie i filtrowanie takich struktur za pomocą **GPath**.

GPath pozwala nawigować po zagnieżdżonych właściwościach i elementach
kolekcji w zwięzły sposób. W tym labie użyjesz go do analizy historii
transakcji: odczytasz pola, wybierzesz wpłaty, pobierzesz wartości
zagnieżdżone i zbudujesz raport.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. Czym jest GPath?

GPath to składnia nawigowania po obiektach, właściwościach i kolekcjach
Groovy. W wielu miejscach przypomina XPath dla XML, ale operuje na
obiektach Groovy i strukturach takich jak mapy oraz listy.

Przykładowa mapa:

```groovy
def account = [
        number: "PLN-1001",
        owner : [name: "Anna Kowalska"],
        tags  : ["personal", "premium"]
]
```

Odczyt zagnieżdżonej wartości:

```groovy
account.owner.name
```

Nawigacja po kolekcji zwraca kolekcję wartości:

```groovy
def numbers = accounts.number
```

To samo można zapisać jawniej:

```groovy
def numbers = accounts.collect { account -> account.number }
```

GPath jest skrótem poprawiającym czytelność, nie osobnym typem danych.

## 2. Nawigacja po liście map

Załóżmy, że mamy transakcje jako listę map:

```groovy
def transactions = [
        [id: 1, type: "DEPOSIT", amount: 100.00],
        [id: 2, type: "WITHDRAWAL", amount: 25.00]
]
```

GPath może zebrać wartości wybranej właściwości:

```groovy
transactions.type
// ["DEPOSIT", "WITHDRAWAL"]

transactions.amount
// lista kwot
```

W praktyce kwoty finansowe w modelu aplikacji powinny być
`BigDecimal`. Powyższy skrót pokazuje jedynie nawigację po strukturze.

## 3. Wybieranie elementów

Do filtrowania list w Groovy użyj `findAll`:

```groovy
def deposits = transactions.findAll { tx ->
    tx.type == "DEPOSIT"
}
```

Po filtrowaniu można użyć GPath, aby zebrać kwoty:

```groovy
def depositAmounts = deposits.amount
```

Albo połączyć oba kroki:

```groovy
def depositAmounts = transactions
        .findAll { tx -> tx.type == "DEPOSIT" }
        .amount
```

Jeśli closure staje się bardziej złożone, jawny parametr `tx` zwykle
czyta się łatwiej niż skróty. Temat closures i metod kolekcji był
omawiany w Labie 25.

## 4. Nawigacja po strukturze JSON

Po parsowaniu JSON-u przez `JsonSlurper` zagnieżdżony dokument zwykle
staje się mapą zawierającą kolejne mapy i listy:

```groovy
def document = new JsonSlurper().parseText(json)

document.account.number
document.transactions.type
```

GPath jest wygodny, ale nie powinien zastępować walidacji:

- sprawdź, czy pola wymagane istnieją,
- zweryfikuj typy,
- zdecyduj, co ma się stać z `null`,
- sprawdź zakresy i reguły domenowe.

Jeżeli struktura pochodzi z zewnętrznego JSON-u i nie jest zaufana,
traktuj odczyt pól jako etap po parsowaniu, a nie dowód, że dokument
ma oczekiwany schemat.

## 5. Dane raportu

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\gpath
```

Utwórz plik `TransactionReportReader.groovy`:

```groovy
package com.delta.bank.lab.groovy.gpath

import groovy.json.JsonSlurper

import java.math.BigDecimal
import java.util.Objects

class TransactionReportReader {

    Map read(String json) {
        Objects.requireNonNull(json, "json must not be null")

        def parsed = new JsonSlurper().parseText(json)
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("JSON root must be an object")
        }

        return (Map) parsed
    }

    String accountNumber(Map document) {
        Objects.requireNonNull(document, "document must not be null")

        def account = document["account"]
        if (!(account instanceof Map)) {
            throw new IllegalArgumentException("account must be an object")
        }

        def number = account["number"]
        if (!(number instanceof String) || number.isBlank()) {
            throw new IllegalArgumentException(
                    "account.number must be a non-blank String"
            )
        }

        return number
    }

    List<String> transactionTypes(Map document) {
        List<Map> transactions = transactions(document)

        return transactions.type.collect { Object type ->
            if (!(type instanceof String)) {
                throw new IllegalArgumentException(
                        "transaction.type must be a String"
                )
            }
            return (String) type
        }
    }

    List<BigDecimal> depositAmounts(Map document) {
        List<Map> deposits = transactions(document).findAll { Map tx ->
            tx["type"] == "DEPOSIT"
        }

        return deposits.collect { Map tx ->
            Object value = tx["amount"]
            if (!(value instanceof Number)) {
                throw new IllegalArgumentException(
                        "transaction.amount must be a JSON number"
                )
            }
            return new BigDecimal(value.toString())
        }
    }

    private static List<Map> transactions(Map document) {
        Objects.requireNonNull(document, "document must not be null")

        def value = document["transactions"]
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("transactions must be an array")
        }

        List<Map> result = []
        for (Object element : (List) value) {
            if (!(element instanceof Map)) {
                throw new IllegalArgumentException(
                        "each transaction must be an object"
                )
            }
            result.add((Map) element)
        }

        return result
    }
}
```

W `transactionTypes` wyrażenie `transactions.type` jest użyciem GPath.
W `depositAmounts` łączysz filtrowanie `findAll` z nawigacją i
przekształceniem `collect`.

Metoda `transactions(...)` sprawdza strukturę tablicy przed dalszym
użyciem. Sam GPath nie sprawdza, czy wejściowy JSON ma poprawne typy
i wymagane pola.

## 6. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\gpath
```

Utwórz w nim `TransactionReportReaderSpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.gpath

import spock.lang.Specification

import java.math.BigDecimal

class TransactionReportReaderSpec extends Specification {

    private final TransactionReportReader reader =
            new TransactionReportReader()

    def "reads a nested account number with GPath"() {
        expect:
        reader.accountNumber(reader.read(reportJson())) == "PLN-1001"
    }

    def "collects transaction types through GPath"() {
        expect:
        reader.transactionTypes(reader.read(reportJson())) ==
                ["DEPOSIT", "WITHDRAWAL", "DEPOSIT"]
    }

    def "filters deposits and converts their amounts to BigDecimal"() {
        when:
        def amounts = reader.depositAmounts(reader.read(reportJson()))

        then:
        amounts.size() == 2
        amounts[0].compareTo(new BigDecimal("100.00")) == 0
        amounts[1].compareTo(new BigDecimal("25.50")) == 0
    }

    def "rejects a non-object JSON root"() {
        when:
        reader.read("[]")

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "JSON root must be an object"
    }

    def "rejects a missing account object"() {
        when:
        reader.accountNumber([:])

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "account must be an object"
    }

    def "rejects an invalid transactions collection"() {
        when:
        reader.transactionTypes([transactions: "not-an-array"])

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "transactions must be an array"
    }

    def "rejects a non-object transaction element"() {
        when:
        reader.transactionTypes([transactions: ["not-an-object"]])

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "each transaction must be an object"
    }

    def "rejects a non-numeric deposit amount"() {
        given:
        def document = [
                transactions: [
                        [type: "DEPOSIT", amount: "100.00"]
                ]
        ]

        when:
        reader.depositAmounts(document)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "transaction.amount must be a JSON number"
    }

    private static String reportJson() {
        '''
        {
          "account": {
            "number": "PLN-1001",
            "owner": "Anna Kowalska"
          },
          "transactions": [
            {"type": "DEPOSIT", "amount": 100.00},
            {"type": "WITHDRAWAL", "amount": 20.00},
            {"type": "DEPOSIT", "amount": 25.50}
          ]
        }
        '''
    }
}
```

Testy sprawdzają zarówno poprawne zagnieżdżone dane, jak i kilka błędnych
kształtów wejścia. Zwróć uwagę, że `amount: "100.00"` jest tekstem JSON,
a `amount: 100.00` jest liczbą — reader przyjmuje tylko tę drugą postać.

## 7. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.gpath.TransactionReportReaderSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.gpath.TransactionReportReaderSpec"
```

Test nie wymaga uruchomienia kontenera PostgreSQL.

## 8. Eksperymenty

### Eksperyment A: zastąp GPath przez `collect`

W `transactionTypes` zamień:

```groovy
transactions.type
```

na:

```groovy
transactions.collect { Map tx -> tx["type"] }
```

Uruchom testy. Wynik powinien być taki sam. Porównaj, która wersja jest
czytelniejsza i kiedy jawna closure pomaga.

### Eksperyment B: sprawdź pustą listę

Przekaż:

```groovy
[transactions: []]
```

do `transactionTypes` i `depositAmounts`. Oczekiwane wyniki to puste
listy. Dopisz testy i sprawdź, czy implementacja zachowuje ten kontrakt.

### Eksperyment C: pole opcjonalne

Dodaj do JSON-u opcjonalne pole `description` w transakcji. Zaimplementuj
metodę, która zbiera opisy, pomijając te brakujące, ale nie pomijając
niepoprawnych typów. Zdecyduj, czy `null`, pusty tekst i brak klucza
mają tę samą semantykę.

### Eksperyment D: odczytaj kwotę bez filtra

Dodaj metodę, która zbiera wszystkie kwoty. Porównaj wynik z
`depositAmounts` i wyjaśnij różnicę między nawigacją po liście a
filtrowaniem listy.

## 9. Dobre praktyki

- Używaj GPath do zwięzłej nawigacji po znanej strukturze.
- Waliduj zewnętrzny JSON przed traktowaniem pól jako typowanych danych.
- Nie zakładaj, że wszystkie elementy kolekcji mają prawidłowy kształt.
- W złożonej logice preferuj nazwaną closure zamiast bardzo długiego
  łańcucha nawigacji.
- Kwoty konwertuj do `BigDecimal`, a nie `double`.
- Traktuj GPath jako składnię dostępu, nie jako walidator schematu.

## Pytania kontrolne

1. Co oznacza `document.account.number` dla mapy zagnieżdżonej?
2. Co zwraca `transactions.type`, gdy `transactions` jest listą map?
3. Jaka jest rola `findAll` i `collect` w `depositAmounts`?
4. Czy GPath sprawdza, że `transactions` rzeczywiście jest listą?
5. Dlaczego parser rozróżnia JSON-ową liczbę od JSON-owego stringa?
6. Kiedy jawna closure może być czytelniejsza niż GPath?
7. Dlaczego odczyt pola nie zastępuje walidacji danych?

Do zapamiętania:

```text
GPath              → nawigacja po właściwościach i kolekcjach
list.property      → zbierz property z elementów listy
findAll { ... }    → wybierz elementy spełniające warunek
collect { ... }    → przekształć elementy
JsonSlurper        → parsowanie składni JSON-u
walidacja schematu → osobny krok po parsowaniu
```
