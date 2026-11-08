# Lab 29 — Groovy: delegowanie closures i własny DSL

## Dlaczego ten temat?

Lab 25 pokazał closures przekazywane do metod kolekcji, Lab 26 —
operatory bezpiecznego dostępu, a Lab 28 — mapy i nazwane argumenty.
Teraz poznasz mechanizm, który pozwala budować czytelne DSL-e Groovy:
delegowanie właściwości i metod z closure do innego obiektu.

Przećwiczysz:

- różnicę między `this`, `owner` i `delegate`,
- strategie rozwiązywania nazw closure,
- `rehydrate(...)` i bezpieczną konfigurację DSL,
- adnotację `@DelegatesTo` dla narzędzi i kontroli typów,
- testowanie poprawnej konfiguracji oraz błędnych nazw właściwości.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. `this`, `owner` i `delegate`

Closure Groovy udostępnia trzy istotne referencje:

- `thisObject` (`this` wewnątrz closure) — obiekt, na którym closure
  zostało utworzone,
- `owner` — bezpośredni obiekt lub closure, w którego kontekście
  zdefiniowano dane closure,
- `delegate` — obiekt używany do rozwiązywania części właściwości
  i wywołań; można go zmienić.

W zwykłym closure `owner` i `delegate` często wskazują początkowo na ten
sam obiekt. W DSL zmieniamy delegate, aby zapis taki jak:

```groovy
fromAccount = "PLN-1001"
amount = new BigDecimal("25.00")
```

ustawiał pola obiektu konfiguracji zamiast lokalnych pól klasy testowej
lub serwisu.

## 2. Strategie rozwiązywania nazw

Closure ma strategię określającą, w jakiej kolejności szuka właściwości
i metod. Najczęściej używane strategie to:

| Strategia | Kolejność rozwiązywania |
|---|---|
| `OWNER_FIRST` | najpierw `owner`, potem `delegate` (domyślna) |
| `DELEGATE_FIRST` | najpierw `delegate`, potem `owner` |
| `OWNER_ONLY` | tylko `owner` |
| `DELEGATE_ONLY` | tylko `delegate` |

Przykład konfliktu nazw:

```groovy
class LabelContext {
    String label
}

def owner = new LabelContext(label: "owner")
def delegate = new LabelContext(label: "delegate")
Closure readLabel = { label }

def ownerFirst = readLabel.rehydrate(delegate, owner, owner)
ownerFirst.resolveStrategy = Closure.OWNER_FIRST
assert ownerFirst() == "owner"

def delegateFirst = readLabel.rehydrate(delegate, owner, owner)
delegateFirst.resolveStrategy = Closure.DELEGATE_FIRST
assert delegateFirst() == "delegate"
```

W kodzie DSL wybierz strategię świadomie. `DELEGATE_ONLY` ułatwia
wykrywanie literówek, bo niezdefiniowana właściwość nie zostanie
przypadkiem odczytana z obiektu `owner`.

## 3. `rehydrate(...)` i `@DelegatesTo`

Metoda `rehydrate(newDelegate, newOwner, newThisObject)` tworzy kopię
closure z podmienionymi kontekstami. W tym labie:

- oryginalny closure pozostaje nietknięty,
- kopia otrzymuje builder jako delegate,
- `owner` i `thisObject` zachowują pierwotny kontekst wywołującego.

Adnotacja `@DelegatesTo` opisuje kompilatorowi i IDE, jaki typ będzie
delegate closure:

```groovy
TransferDraft transfer(
        @DelegatesTo(
                value = TransferDraftBuilder,
                strategy = Closure.DELEGATE_ONLY
        )
        Closure<?> specification
)
```

`@DelegatesTo` nie ustawia delegate podczas wykonania i nie waliduje DSL
samo z siebie. To metoda musi użyć `rehydrate(...)` i ustawić
`resolveStrategy`.

## 4. Model transferu

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\delegation
```

Utwórz plik `TransferDraft.groovy`:

```groovy
package com.delta.bank.lab.groovy.delegation

import java.math.BigDecimal

class TransferDraft {
    final String fromAccount
    final String toAccount
    final BigDecimal amount
    final String memo

    TransferDraft(
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String memo
    ) {
        this.fromAccount = fromAccount
        this.toAccount = toAccount
        this.amount = amount
        this.memo = memo
    }
}
```

Utwórz plik `TransferDraftBuilder.groovy`:

```groovy
package com.delta.bank.lab.groovy.delegation

import java.math.BigDecimal

class TransferDraftBuilder {
    String fromAccount
    String toAccount
    BigDecimal amount
    String memo = ""

    TransferDraft build() {
        if (fromAccount == null || fromAccount.isBlank()) {
            throw new IllegalArgumentException("fromAccount is required")
        }
        if (toAccount == null || toAccount.isBlank()) {
            throw new IllegalArgumentException("toAccount is required")
        }
        if (fromAccount == toAccount) {
            throw new IllegalArgumentException(
                    "fromAccount and toAccount must be different"
            )
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required")
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero")
        }
        if (memo == null) {
            throw new IllegalArgumentException("memo must not be null")
        }

        return new TransferDraft(fromAccount, toAccount, amount, memo)
    }
}
```

Builder przechowuje konfigurację przejściową. Walidacja jest wykonywana
podczas `build()`, dzięki czemu nie powstanie niekompletny `TransferDraft`.

## 5. Metoda DSL

Utwórz `TransferDsl.groovy` w tym samym katalogu:

```groovy
package com.delta.bank.lab.groovy.delegation

import groovy.lang.Closure
import groovy.lang.DelegatesTo

import java.util.Objects

class TransferDsl {

    TransferDraft transfer(
            @DelegatesTo(
                    value = TransferDraftBuilder,
                    strategy = Closure.DELEGATE_ONLY
            )
            Closure<?> specification
    ) {
        Objects.requireNonNull(
                specification,
                "specification must not be null"
        )

        def builder = new TransferDraftBuilder()
        Closure<?> configured = specification.rehydrate(
                builder,
                specification.owner,
                specification.thisObject
        )
        configured.resolveStrategy = Closure.DELEGATE_ONLY
        configured.call()

        return builder.build()
    }
}
```

Po tej implementacji można zapisać:

```groovy
def draft = new TransferDsl().transfer {
    fromAccount = "PLN-1001"
    toAccount = "PLN-1002"
    amount = new BigDecimal("25.00")
    memo = "rent"
}
```

Wewnątrz closure przypisania trafiają do `TransferDraftBuilder`.
Oryginalny closure nie jest modyfikowany: metoda konfiguruje i wykonuje
jego kopię zwróconą przez `rehydrate(...)`.

## 6. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\delegation
```

Utwórz w nim `TransferDslSpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.delegation

import spock.lang.Specification

import java.math.BigDecimal

class TransferDslSpec extends Specification {

    private final TransferDsl dsl = new TransferDsl()
    String fromAcccount = "owner value"

    def "delegates configuration closure to the builder"() {
        when:
        def draft = dsl.transfer {
            fromAccount = "PLN-1001"
            toAccount = "PLN-1002"
            amount = new BigDecimal("25.00")
            memo = "rent"
        }

        then:
        draft.fromAccount == "PLN-1001"
        draft.toAccount == "PLN-1002"
        draft.amount.compareTo(new BigDecimal("25.00")) == 0
        draft.memo == "rent"
    }

    def "uses an empty memo when it is omitted"() {
        when:
        def draft = dsl.transfer {
            fromAccount = "PLN-1001"
            toAccount = "PLN-1002"
            amount = new BigDecimal("1.00")
        }

        then:
        draft.memo == ""
    }

    def "does not change the original closure delegate or strategy"() {
        given:
        Closure<?> specification = {
            fromAccount = "PLN-1001"
            toAccount = "PLN-1002"
            amount = new BigDecimal("10.00")
        }
        def originalDelegate = specification.delegate
        def originalStrategy = specification.resolveStrategy

        when:
        dsl.transfer(specification)

        then:
        specification.delegate.is(originalDelegate)
        specification.resolveStrategy == originalStrategy
    }

    def "fails for an unknown property instead of using the owner"() {
        when:
        dsl.transfer {
            fromAcccount = "PLN-1001"
            toAccount = "PLN-1002"
            amount = new BigDecimal("10.00")
        }

        then:
        thrown(MissingPropertyException)
        fromAcccount == "owner value"
    }

    def "rejects a transfer to the same account"() {
        when:
        dsl.transfer {
            fromAccount = "PLN-1001"
            toAccount = "PLN-1001"
            amount = new BigDecimal("10.00")
        }

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "fromAccount and toAccount must be different"
    }

    def "rejects a non-positive amount"() {
        when:
        dsl.transfer {
            fromAccount = "PLN-1001"
            toAccount = "PLN-1002"
            amount = BigDecimal.ZERO
        }

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "amount must be greater than zero"
    }

    def "rejects an incomplete transfer"() {
        when:
        dsl.transfer {
            fromAccount = "PLN-1001"
            amount = new BigDecimal("10.00")
        }

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "toAccount is required"
    }
}
```

Literówka `fromAcccount` powinna dać `MissingPropertyException`. Przy
`DELEGATE_ONLY` closure nie szuka brakującej nazwy w obiekcie speca.

## 7. Uruchomienie

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.delegation.TransferDslSpec"
```

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.groovy.delegation.TransferDslSpec"
```

Test nie wymaga uruchomienia kontenera PostgreSQL.

## 8. Eksperymenty

### Eksperyment A: zmień strategię na `OWNER_FIRST`

Ustaw na skonfigurowanej kopii:

```groovy
configured.resolveStrategy = Closure.OWNER_FIRST
```

Uruchom test z literówką. Sprawdź, czy nazwa może zostać rozwiązana
w kontekście ownera: spec ma pole o dokładnie błędnej nazwie
`fromAcccount`. Przy `OWNER_FIRST` przypisanie trafi do tego pola, więc
builder nie dostanie wymaganego `fromAccount` i walidacja zgłosi inny
wyjątek. Wyjaśnij różnicę między kolejnością `OWNER_FIRST` a ograniczeniem
`DELEGATE_ONLY`.

### Eksperyment B: zmień kolejność argumentów `rehydrate`

API przyjmuje argumenty w kolejności:

```groovy
rehydrate(newDelegate, newOwner, newThisObject)
```

Zmień pierwszy argument z buildera na inny obiekt i sprawdź, co dzieje
się z przypisaniami `fromAccount` i `amount`. Przywróć builder jako
delegate.

### Eksperyment C: usuń `rehydrate`

Uruchom oryginalne `specification` bez tworzenia kopii i ustaw strategię
bezpośrednio na nim. Test sprawdzający niezmieniony delegate/strategy
powinien ujawnić, że zmieniłeś obiekt przekazany przez wywołującego.
Przywróć wersję z `rehydrate(...)`.

## 9. `@DelegatesTo` i statyczna kontrola

`@DelegatesTo` pomaga IDE podpowiadać właściwości buildera wewnątrz
closure. Może też przekazać informację o delegate do statycznej kontroli
typów Groovy, ale nie zmienia dynamicznego wyszukiwania nazw w runtime.

Adnotacja i implementacja muszą być spójne:

```text
@DelegatesTo → opisuje oczekiwany delegate
rehydrate    → ustawia delegate kopii closure
resolveStrategy → wybiera kolejność wyszukiwania nazw
```

Jeśli zmienisz tylko adnotację, nie zmienisz zachowania wykonywanego DSL.

## Pytania kontrolne

1. Czym `delegate` różni się od `owner`?
2. Jaka jest domyślna strategia rozwiązywania nazw closure?
3. Kiedy wybrać `DELEGATE_ONLY`, a kiedy `DELEGATE_FIRST`?
4. Jakie argumenty i w jakiej kolejności przyjmuje `rehydrate(...)`?
5. Czy `@DelegatesTo` samodzielnie ustawia delegate podczas wykonania?
6. Dlaczego metoda DSL używa kopii closure zamiast modyfikować oryginał?
7. Co powinno się stać z literówką w nazwie właściwości przy `DELEGATE_ONLY`?
8. Dlaczego walidujemy builder przed utworzeniem `TransferDraft`?

Do zapamiętania:

```text
thisObject → obiekt, na którym utworzono closure
owner      → bezpośredni kontekst definicji closure
delegate   → obiekt używany do rozwiązywania nazw
rehydrate  → nowa closure z wybranym delegate, ownerem i thisObject
@DelegatesTo → wskazówka typów/IDE, nie konfiguracja runtime
DELEGATE_ONLY → nazwy pochodzą wyłącznie z delegate
```
