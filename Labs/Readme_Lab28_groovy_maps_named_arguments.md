# Lab 28 — Groovy: mapy i nazwane argumenty

## Dlaczego ten temat?

Lab 25 ćwiczył closures i kolekcje, Lab 26 — bezpieczny dostęp do
brakujących wartości, a Lab 27 — statyczną kontrolę typów. Teraz
przećwiczysz mapy Groovy jako praktyczny sposób przekazywania nazwanych
opcji do metody.

Zobaczysz:

- składnię mapy i dostęp do jej wartości,
- wywołanie metody z argumentami nazwanymi,
- różnicę między brakującym kluczem a kluczem z wartością `null`,
- łączenie map i walidację nieznanych opcji,
- jak zachować poprawne wartości takie jak `false` i zero.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. Mapa w Groovy

Mapa przechowuje pary klucz-wartość:

```groovy
def options = [
        accountNumber: "PLN-1001",
        currency: "PLN",
        enabled: true
]
```

W tej składni klucze bez cudzysłowów są tekstowymi kluczami mapy. Powyższy
zapis jest skrótem dla mapy z kluczami `"accountNumber"`, `"currency"`
i `"enabled"`.

Możesz odczytać wartość nawiasami:

```groovy
options["accountNumber"]
```

albo notacją właściwości:

```groovy
options.accountNumber
```

Notacja z nawiasami jest jednoznaczna i działa również dla kluczy, które
nie są identyfikatorami, na przykład `options["daily-limit"]`. Przy
konfiguracji i walidacji zwykle preferuj nawiasy.

Pusta mapa:

```groovy
def empty = [:]
```

Mapa utworzona literałem zachowuje kolejność wstawiania wpisów. Jeśli
kolejność ma znaczenie dla logiki, określ i przetestuj ten kontrakt jawnie.

## 2. Nazwane argumenty metod

W Groovy takie wywołanie:

```groovy
createPolicy(
        accountNumber: "PLN-1001",
        currency: "PLN",
        enabled: false
)
```

przekazuje mapę jako pierwszy argument metody. Odpowiednia metoda może
przyjąć ją jawnie:

```groovy
TransferPolicy createPolicy(Map options) {
    // Odczytaj i zwaliduj options
}
```

To konwencja Groovy, a nie osobny typ argumentów nazwanych, który Java
widzi w sygnaturze. Kod Javy wywołujący tę metodę przekazałby zwykły
obiekt `Map`.

Nazwane argumenty poprawiają czytelność wywołania, ale same nie
walidują danych, nie wymuszają wymaganych pól i nie odrzucają
nieznanych kluczy. To należy do implementacji metody.

## 3. Brakujący klucz a `null`

Te mapy nie są równoważne:

```groovy
def omitted = [:]
def explicitlyNull = [dailyLimit: null]
```

W obu przypadkach `map["dailyLimit"]` zwróci `null`. Aby sprawdzić,
czy klucz w ogóle został przekazany, użyj:

```groovy
map.containsKey("dailyLimit")
```

To ważne, gdy:

- brak klucza oznacza zastosowanie wartości domyślnej,
- jawny `null` jest błędem,
- `false` lub zero są prawidłowymi wartościami i nie mogą uruchamiać
  wartości domyślnej.

Nie używaj bez namysłu Elvis `?:` do ustawiania domyślnej wartości.
Jak w Labie 26, Groovy Truth potraktuje `false`, zero, pusty tekst
i pustą kolekcję tak samo jak brak wartości.

## 4. Model polityki transferów

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\maps
```

Utwórz plik `TransferPolicy.groovy`:

```groovy
package com.delta.bank.lab.groovy.maps

import java.math.BigDecimal

class TransferPolicy {
    final String accountNumber
    final String currency
    final BigDecimal dailyLimit
    final boolean enabled

    TransferPolicy(
            String accountNumber,
            String currency,
            BigDecimal dailyLimit,
            boolean enabled
    ) {
        this.accountNumber = accountNumber
        this.currency = currency
        this.dailyLimit = dailyLimit
        this.enabled = enabled
    }
}
```

Pola są `final`, a więc po skonstruowaniu obiektu nie można przypisać
im nowych wartości. Walidację argumentów umieścimy w fabryce.

## 5. Fabryka z walidacją mapy

Utwórz `TransferPolicyFactory.groovy` w tym samym katalogu:

```groovy
package com.delta.bank.lab.groovy.maps

import java.math.BigDecimal
import java.util.Objects

class TransferPolicyFactory {

    private static final Set<String> ALLOWED_KEYS = [
            "accountNumber",
            "currency",
            "dailyLimit",
            "enabled"
    ] as Set<String>

    private static final BigDecimal DEFAULT_DAILY_LIMIT =
            new BigDecimal("1000.00")

    TransferPolicy create(Map options) {
        Objects.requireNonNull(options, "options must not be null")

        def unknownKeys = options.keySet()
                .findAll { key -> !(key instanceof String) || !ALLOWED_KEYS.contains(key) }

        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown option(s): ${unknownKeys.collect { it.toString() }.sort().join(', ')}"
            )
        }

        if (!options.containsKey("accountNumber")) {
            throw new IllegalArgumentException("accountNumber is required")
        }
        if (!options.containsKey("currency")) {
            throw new IllegalArgumentException("currency is required")
        }

        def accountNumberValue = options["accountNumber"]
        if (!(accountNumberValue instanceof String)) {
            throw new IllegalArgumentException("accountNumber must be a String")
        }
        String accountNumber = accountNumberValue
        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank")
        }

        def currencyValue = options["currency"]
        if (!(currencyValue instanceof String)) {
            throw new IllegalArgumentException("currency must be a String")
        }
        String currency = currencyValue
        if (!(currency ==~ /^[A-Z]{3}$/)) {
            throw new IllegalArgumentException(
                    "currency must be a 3-letter uppercase code"
            )
        }

        BigDecimal dailyLimit
        if (!options.containsKey("dailyLimit")) {
            dailyLimit = DEFAULT_DAILY_LIMIT
        } else {
            def limitValue = options["dailyLimit"]
            if (!(limitValue instanceof BigDecimal)) {
                throw new IllegalArgumentException(
                        "dailyLimit must be a BigDecimal"
                )
            }
            dailyLimit = limitValue
        }
        if (dailyLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "dailyLimit must not be negative"
            )
        }

        boolean enabled = true
        if (options.containsKey("enabled")) {
            def enabledValue = options["enabled"]
            if (!(enabledValue instanceof Boolean)) {
                throw new IllegalArgumentException("enabled must be a Boolean")
            }
            enabled = enabledValue
        }

        return new TransferPolicy(
                accountNumber,
                currency,
                dailyLimit,
                enabled
        )
    }
}
```

Kilka szczegółów implementacji:

- `containsKey` odróżnia pominięty limit od jawnego `dailyLimit: null`.
- Domyślny limit jest wybierany tylko wtedy, gdy klucza nie ma.
- `enabled: false` pozostaje `false`; nie jest zastępowane domyślnym `true`.
- Zero jest prawidłowym limitem, a wartość ujemna jest odrzucana.
- Nieznane klucze są jawnie odrzucane zamiast po cichu ignorowane.
- Limit musi być `BigDecimal`; fabryka nie konwertuje automatycznie
  tekstu ani `double`.

## 6. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\maps
```

Utwórz w nim `TransferPolicyFactorySpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.maps

import spock.lang.Specification
import spock.lang.Unroll

import java.math.BigDecimal

class TransferPolicyFactorySpec extends Specification {

    private final TransferPolicyFactory factory =
            new TransferPolicyFactory()

    def "named arguments create a policy"() {
        when:
        def policy = factory.create(
                accountNumber: "PLN-1001",
                currency: "PLN",
                dailyLimit: new BigDecimal("250.00"),
                enabled: true
        )

        then:
        policy.accountNumber == "PLN-1001"
        policy.currency == "PLN"
        policy.dailyLimit.compareTo(new BigDecimal("250.00")) == 0
        policy.enabled
    }

    def "uses defaults only for omitted optional keys"() {
        when:
        def policy = factory.create(
                accountNumber: "PLN-1001",
                currency: "PLN"
        )

        then:
        policy.dailyLimit.compareTo(new BigDecimal("1000.00")) == 0
        policy.enabled
    }

    def "preserves false and zero instead of using defaults"() {
        when:
        def policy = factory.create(
                accountNumber: "PLN-1001",
                currency: "PLN",
                dailyLimit: BigDecimal.ZERO,
                enabled: false
        )

        then:
        policy.dailyLimit.compareTo(BigDecimal.ZERO) == 0
        !policy.enabled
    }

    def "allows assembling options from maps"() {
        given:
        def required = [
                accountNumber: "PLN-1001",
                currency: "PLN"
        ]
        def overrides = [
                dailyLimit: new BigDecimal("75.00"),
                enabled: false
        ]

        when:
        def policy = factory.create(required + overrides)

        then:
        policy.dailyLimit.compareTo(new BigDecimal("75.00")) == 0
        !policy.enabled
    }

    def "rejects a missing required key"() {
        when:
        factory.create(currency: "PLN")

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "accountNumber is required"
    }

    @Unroll
    def "rejects invalid option #caseName"() {
        when:
        factory.create(options)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == expectedMessage

        where:
        caseName            | options                                                        | expectedMessage
        "null account"      | [accountNumber: null, currency: "PLN"]                        | "accountNumber must be a String"
        "blank account"     | [accountNumber: "  ", currency: "PLN"]                        | "accountNumber must not be blank"
        "lowercase currency"| [accountNumber: "PLN-1001", currency: "pln"]                  | "currency must be a 3-letter uppercase code"
        "null limit"        | [accountNumber: "PLN-1001", currency: "PLN", dailyLimit: null] | "dailyLimit must be a BigDecimal"
        "negative limit"    | [accountNumber: "PLN-1001", currency: "PLN", dailyLimit: new BigDecimal("-1.00")] | "dailyLimit must not be negative"
        "string limit"      | [accountNumber: "PLN-1001", currency: "PLN", dailyLimit: "100.00"] | "dailyLimit must be a BigDecimal"
        "null enabled"      | [accountNumber: "PLN-1001", currency: "PLN", enabled: null]   | "enabled must be a Boolean"
        "unknown option"    | [accountNumber: "PLN-1001", currency: "PLN", enabeld: true]  | "Unknown option(s): enabeld"
    }

    def "rejects a null options map"() {
        when:
        factory.create(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "options must not be null"
    }
}
```

W teście z `required + overrides` operator `+` tworzy nową mapę. Jeśli
ten sam klucz znajduje się po obu stronach, wartość z prawej mapy
zastępuje wartość z lewej.

## 7. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.maps.TransferPolicyFactorySpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.maps.TransferPolicyFactorySpec"
```

Test nie wymaga uruchomienia kontenera PostgreSQL.

## 8. Eksperymenty

### Eksperyment A: użyj Elvis dla wartości domyślnych

Zmień wybór `dailyLimit` tak, aby korzystał z:

```groovy
options["dailyLimit"] ?: DEFAULT_DAILY_LIMIT
```

Uruchom test `preserves false and zero instead of using defaults`.
Zobacz, dlaczego poprawny limit zero zostaje zastąpiony wartością
domyślną. Przywróć wersję opartą o `containsKey`.

### Eksperyment B: pomiń walidację nieznanych kluczy

Usuń sprawdzanie `unknownKeys` i uruchom spec. Test literówki
`enabeld` przestanie przechodzić, ale fabryka utworzy politykę
z domyślnym `enabled: true`. Przywróć walidację.

### Eksperyment C: sprawdź regułę nadpisywania map

Dodaj `enabled: true` do mapy `required`, pozostaw `enabled: false`
w `overrides` i sprawdź wynik. Następnie zamień kolejność map w wyrażeniu
`required + overrides` i wyjaśnij zmianę wyniku.

## 9. Dobre praktyki

- Używaj nazwanych argumentów, gdy poprawiają czytelność wywołania.
- Traktuj mapę wejściową jak dane zewnętrzne: waliduj wymagane klucze,
  typy, zakresy i nieznane opcje.
- Użyj `containsKey`, gdy pominięty klucz różni się semantycznie od
  klucza z wartością `null`.
- Nie używaj Elvis, jeśli `false`, zero lub pusty tekst są poprawnymi
  wartościami.
- Unikaj przekazywania niekontrolowanych map głęboko przez aplikację.
  Po walidacji zamień je na obiekt z jasno określonymi polami i typami.

## Pytania kontrolne

1. Jaki argument otrzymuje metoda wywołana jako `create(name: "value")`?
2. Jak sprawdzić, czy mapa zawiera klucz o wartości `null`?
3. Jaka jest różnica między kluczem pominiętym a kluczem ustawionym na
   `null` w tym ćwiczeniu?
4. Dlaczego `enabled: false` nie powinno być zastępowane domyślnym `true`?
5. Jak działa operator `+` dla dwóch map z tym samym kluczem?
6. Dlaczego fabryka odrzuca nieznane klucze?
7. Dlaczego sprawdzenie typu jest potrzebne, mimo że wywołanie używa
   nazwanych argumentów?
8. Kiedy warto po walidacji mapy utworzyć obiekt z typowanymi polami?

Do zapamiętania:

```text
[key: value]                → mapa Groovy
create(name: "x")           → mapa przekazana jako pierwszy argument
containsKey(key)            → odróżnia brak klucza od klucza z null
left + right                → nowa mapa; przy kolizji wygrywa prawa mapa
walidacja mapy              → wymagane klucze, typy, zakresy i nieznane opcje
false / zero                → nie zastępuj Elvisem, jeśli są prawidłowymi danymi
```
