# Lab 30 — Groovy DSL: jak działa konfiguracja transferu?

## Cel

W Labie 29 zbudowałeś mały DSL do opisywania transferu. Ten materiał
porządkuje jego działanie: wyjaśnia, skąd bierze się czytelna składnia
oraz jak `Closure`, `delegate`, `rehydrate(...)` i builder współpracują
ze sobą.

Nie dodawaj ani nie zmieniaj kodu. To uzupełnienie wyjaśniające Lab 29.

## 1. Co oznacza DSL?

**DSL** (*Domain-Specific Language*) to mały język zaprojektowany
dla konkretnej dziedziny.

W Labie 29 opis transferu wygląda tak:

```groovy
def draft = new TransferDsl().transfer {
    fromAccount = "PLN-1001"
    toAccount = "PLN-1002"
    amount = new BigDecimal("25.00")
    memo = "rent"
}
```

Widzisz tu właściwości, które opisują transfer, zamiast listy
argumentów pozycyjnych:

```groovy
new TransferDraftBuilder().configure(
        "PLN-1001",
        "PLN-1002",
        new BigDecimal("25.00"),
        "rent"
)
```

To nadal zwykły Groovy — nie dodajesz nowej składni do języka.
DSL powstaje dzięki temu, jak metoda `transfer` przyjmuje i wykonuje
closure.

## 2. Co dzieje się krok po kroku?

Przepływ można przedstawić tak:

```text
transfer { ... }
      │
      ▼
Closure przekazane do TransferDsl.transfer(...)
      │
      ▼
TransferDraftBuilder zostaje ustawiony jako delegate kopii closure
      │
      ▼
configured.call() wypełnia pola buildera
      │
      ▼
builder.build() waliduje dane i tworzy TransferDraft
```

1. `transfer { ... }` przekazuje blok `{ ... }` jako argument typu
   `Closure`.
2. `TransferDsl.transfer` tworzy `TransferDraftBuilder` z polami
   `fromAccount`, `toAccount`, `amount` i `memo`.
3. `rehydrate(...)` tworzy kopię closure i ustawia builder jako jej
   `delegate`.
4. `DELEGATE_ONLY` każe Groovy szukać nazw z closure, takich jak
   `fromAccount`, wyłącznie w builderze. Literówka, np. `fromAcccount`,
   nie zostanie omyłkowo potraktowana jako pole innego obiektu —
   zakończy się błędem.
5. `configured.call()` wykonuje skonfigurowaną kopię closure, która
   wypełnia pola buildera.
6. `builder.build()` sprawdza wymagane dane i reguły, a następnie
   tworzy obiekt `TransferDraft`.

## 3. Rola poszczególnych elementów

### `delegate`

Wskazuje obiekt, na którym closure ma operować. W tym DSL-u jest nim
`TransferDraftBuilder`, więc przypisanie:

```groovy
fromAccount = "PLN-1001"
```

ustawia właściwość buildera.

### `DELEGATE_ONLY`

Określa, gdzie Groovy ma szukać właściwości i metod użytych w closure.
Strategia `DELEGATE_ONLY` ogranicza wyszukiwanie do obiektu `delegate`.
Pomaga to ujawniać literówki zamiast przypadkowo odczytywać lub zmieniać
wartości w kontekście wywołującego.

### `rehydrate(...)`

Tworzy kopię closure z ustawionymi kontekstami, między innymi `delegate`.
Dzięki temu DSL może skonfigurować i wykonać kopię, nie zmieniając
oryginalnego closure przekazanego przez wywołującego.

### `@DelegatesTo`

Informuje IDE i narzędzia, jaki typ obiektu będzie delegate closure.
Może dzięki temu zapewnić podpowiedzi dotyczące właściwości buildera.
Sama adnotacja **nie ustawia delegate podczas wykonania** — robi to
implementacja metody, używając `rehydrate(...)`.

### `build()`

Builder zbiera wartości przekazane w closure. Metoda `build()` sprawdza,
czy konfiguracja jest kompletna i poprawna, a dopiero potem tworzy
obiekt wynikowy `TransferDraft`.

## 4. Najważniejsza idea

```text
closure opisuje konfigurację
builder zbiera wartości
build() waliduje je i tworzy obiekt wynikowy
```

DSL jest przydatny wtedy, gdy taki opis staje się czytelniejszy od
długiego wywołania z wieloma argumentami. Warto jednak pamiętać, że
czytelność składni nie zastępuje walidacji — wymagane pola i reguły
muszą być sprawdzane przez builder.

## Pytania kontrolne

1. Co sprawia, że zapis `transfer { ... }` jest DSL-em, mimo że to
   zwykły Groovy?
2. Jaki obiekt jest `delegate` closure w tym przykładzie?
3. Co robi strategia `DELEGATE_ONLY`?
4. Dlaczego metoda używa kopii closure utworzonej przez `rehydrate(...)`?
5. Czy `@DelegatesTo` samodzielnie ustawia delegate w czasie wykonania?
6. Za co odpowiada `builder.build()`?

Do zapamiętania:

```text
DSL           → czytelny zapis dopasowany do konkretnej dziedziny
Closure       → przekazany blok konfiguracji
delegate      → obiekt, na którym closure operuje
DELEGATE_ONLY → nazwy są rozwiązywane wyłącznie w delegate
rehydrate     → kopia closure z ustawionymi kontekstami
@DelegatesTo  → podpowiedź dla IDE i kontroli typów
build()       → walidacja konfiguracji i utworzenie wyniku
```
