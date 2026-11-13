# Lab 36 — Pliki UTF-8 i testy Spocka z `@TempDir`

## Dlaczego ten temat?

Laby 34 i 35 przetwarzały dane zapisane w tekście. Teraz wykonasz
kolejny praktyczny krok: zapiszesz raport do pliku i odczytasz go z dysku.

Poznasz:

- `Path` i `resolve(...)` do budowania ścieżek,
- `Files.writeString(...)` i `Files.readString(...)`,
- jawny wybór kodowania UTF-8,
- zachowanie zapisu do nowego i istniejącego pliku,
- `@TempDir` w Spocku, czyli izolowane katalogi testowe,
- testowanie błędów wejścia/wyjścia bez ukrywania wyjątków.

API odczytu i zapisu tekstu użyte w tym ćwiczeniu istnieje od Java 11.
Używamy go z Groovy na Java 21 — nie jest to nowa funkcja Java 21.

Dodaj tylko wskazane pliki szkoleniowe. Nie zmieniaj kodu produkcyjnego,
konfiguracji Gradle ani istniejących labów. Ćwiczenie działa bez Springa,
bazy danych, sieci i preview Java.

## 1. `Path` opisuje ścieżkę, a `Files` wykonuje operację

```groovy
Path directory = Path.of("reports")
Path report = directory.resolve("statement.txt")
```

`resolve(...)` pozwala zbudować ścieżkę bez ręcznego sklejania separatorów.
Wynik nie tworzy jeszcze katalogu ani pliku.

Operacje wykonujesz przez `Files`:

```groovy
Files.createDirectories(directory)
Files.writeString(report, "Report", StandardCharsets.UTF_8)
```

Nie zapisuj testowych raportów w katalogu projektu. W testach użyjemy
katalogu tymczasowego zarządzanego przez Spocka.

## 2. Tekst a bajty — po co UTF-8?

Plik przechowuje bajty. Kodowanie określa, jak przekształcić tekst
na bajty i jak odtworzyć tekst podczas odczytu.

```groovy
Files.writeString(path, content, StandardCharsets.UTF_8)
Files.readString(path, StandardCharsets.UTF_8)
```

Ten sam wybór kodowania po obu stronach pozwala poprawnie odczytać
polskie znaki i inne znaki spoza ASCII.

Warianty tych metod bez argumentu kodowania także używają UTF-8.
W tym labie podajemy je jawnie, aby kontrakt był widoczny.

## 3. Co robi domyślny zapis?

`Files.writeString(...)` bez dodatkowych opcji:

- tworzy plik, jeśli go nie ma,
- nadpisuje zawartość istniejącego pliku,
- usuwa poprzednią końcówkę, gdy nowy tekst jest krótszy,
- nie tworzy automatycznie brakujących katalogów nadrzędnych.

Nie dopisuje też samodzielnie znaku nowej linii.

To świadomy kontrakt naszego ćwiczenia: zapis zastępuje cały raport.
Nie dodajemy `APPEND` ani automatycznego tworzenia katalogów.

## 4. Klasa do zapisu i odczytu

Utwórz katalog:

```text
src\main\groovy\com\delta\bank\lab\groovy\files
```

Utwórz plik `StatementFileStore.groovy`:

```groovy
package com.delta.bank.lab.groovy.files

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Objects

class StatementFileStore {

    void write(Path path, String content) {
        Objects.requireNonNull(path, "path must not be null")
        Objects.requireNonNull(content, "content must not be null")

        Files.writeString(path, content, StandardCharsets.UTF_8)
    }

    String read(Path path) {
        Objects.requireNonNull(path, "path must not be null")

        return Files.readString(path, StandardCharsets.UTF_8)
    }
}
```

Nie łapiemy tutaj wyjątków wejścia/wyjścia. Jeśli pliku nie ma albo
zapis się nie powiedzie, wywołujący otrzyma wyjątek zamiast pustego tekstu
lub pozornego sukcesu.

Te metody otwierają i zamykają potrzebne zasoby wewnętrznie. Nie dodajesz
wokół nich własnego `try-with-resources`. Inaczej jest przy API, które
zwraca otwarty strumień, np. `Files.lines(...)` — taki zasób trzeba zamknąć.

`readString(...)` wczytuje cały plik do pamięci. To wygodne dla małego
raportu, ale nie jest właściwym wyborem dla pliku o nieograniczonym rozmiarze.

## 5. `@TempDir` — katalog osobny dla testu

W specyfikacji Spocka zadeklarujesz:

```groovy
@TempDir
Path tempDir
```

Spock utworzy katalog i wstrzyknie jego ścieżkę do pola. Przy zwykłym
polu instancji każda metoda testowa otrzyma własny katalog.
Po zakończeniu testu Spock domyślnie posprząta jego zawartość.

Dzięki temu:

- test nie zależy od plików pozostawionych przez poprzednie uruchomienie,
- różne testy nie nadpisują tego samego raportu,
- nie potrzebujesz ręcznego usuwania plików w `cleanup:`.

Nie oznaczaj tego pola jako `@Shared` — w tym labie zależy nam na izolacji.

## 6. Specyfikacja Spocka

Utwórz katalog:

```text
src\test\groovy\com\delta\bank\lab\groovy\files
```

Utwórz w nim `StatementFileStoreSpec.groovy`:

```groovy
package com.delta.bank.lab.groovy.files

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class StatementFileStoreSpec extends Specification {

    @TempDir
    Path tempDir

    private final StatementFileStore store = new StatementFileStore()

    def "writes and reads a report without changing its text"() {
        given:
        Path path = tempDir.resolve("statement.txt")
        String content = "Account: PLN-1001\nBalance: 125.50 PLN\n"

        when:
        store.write(path, content)

        then:
        Files.exists(path)
        store.read(path) == content
    }

    def "writes UTF-8 bytes for Polish characters"() {
        given:
        Path path = tempDir.resolve("polish.txt")
        String content = "Wpłata: Łódź"

        when:
        store.write(path, content)

        then:
        Arrays.equals(
                Files.readAllBytes(path),
                content.getBytes(StandardCharsets.UTF_8)
        )
        store.read(path) == content
    }

    def "overwriting with shorter content removes the old suffix"() {
        given:
        Path path = tempDir.resolve("statement.txt")
        store.write(path, "A long previous report")

        when:
        store.write(path, "New")

        then:
        store.read(path) == "New"
        Files.size(path) == 3L
    }

    def "accepts an empty report"() {
        given:
        Path path = tempDir.resolve("empty.txt")

        when:
        store.write(path, "")

        then:
        Files.size(path) == 0L
        store.read(path) == ""
    }

    def "propagates an error when the requested file does not exist"() {
        given:
        Path path = tempDir.resolve("missing.txt")

        when:
        store.read(path)

        then:
        thrown(NoSuchFileException)
    }

    def "does not create missing parent directories"() {
        given:
        Path directory = tempDir.resolve("missing-directory")
        Path path = directory.resolve("statement.txt")

        when:
        store.write(path, "Report")

        then:
        thrown(NoSuchFileException)
        !Files.exists(directory)
    }

    def "rejects null content before creating a file"() {
        given:
        Path path = tempDir.resolve("statement.txt")

        when:
        store.write(path, null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "content must not be null"
        !Files.exists(path)
    }

    def "rejects a null read path"() {
        when:
        store.read(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "path must not be null"
    }
}
```

Dlaczego test UTF-8 sprawdza również bajty? Sam round-trip nie wystarcza
do wykazania konkretnego kodowania: zapis i odczyt mogłyby używać tego
samego, ale niepoprawnego kodowania. Sprawdzenie bajtów weryfikuje
bezpośrednio wymagany format pliku.

W teście długości pliku używamy tekstu ASCII `"New"`, który zajmuje
dokładnie trzy bajty w UTF-8. Liczba znaków nie jest ogólnie równa
liczbie bajtów — polskie litery mogą zajmować więcej niż jeden bajt.

## 7. Uruchomienie

Z katalogu głównego projektu, w PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.groovy.files.StatementFileStoreSpec"
```

Nie potrzebujesz uruchomionej aplikacji ani kontenera PostgreSQL.
Nie twórz ręcznie katalogu na raporty — zrobi to `@TempDir`.

## 8. Eksperymenty

### Eksperyment A: dopisywanie zamiast zastępowania

Tymczasowo zmień zapis na:

```groovy
Files.writeString(
        path,
        content,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND
)
```

Dodaj import `java.nio.file.StandardOpenOption`. Uruchom spec:
test nadpisania powinien wykryć zmianę kontraktu. Wyjaśnij różnicę
między zastąpieniem raportu a dopisaniem kolejnego fragmentu.

Przywróć poprzedni zapis i usuń niepotrzebny import.

### Eksperyment B: brak końcowej nowej linii

Zapisz `"Report"` i sprawdź, czy odczyt kończy się `"\n"`.
Następnie zapisz `"Report\n"` i porównaj wynik. Dopisz test pokazujący,
że zapis zachowuje tekst, a nie dodaje własnych separatorów.

### Eksperyment C: katalogi nadrzędne

W osobnym teście utwórz katalog przez:

```groovy
Files.createDirectories(tempDir.resolve("reports"))
```

Zapisz raport w tym katalogu. Nie zmieniaj kontraktu `StatementFileStore`:
przygotowanie katalogu nadal należy do wywołującego.

## 9. Dobre praktyki

- Buduj ścieżki przez `Path` i `resolve`, nie przez sklejanie separatorów.
- Testuj pliki w `@TempDir`, a nie w katalogu źródeł projektu.
- Ustal, czy zapis nadpisuje plik, czy dopisuje dane.
- Nie zamieniaj błędu odczytu na pusty raport — pusta zawartość i błąd
  to różne sytuacje.
- Dobieraj API do rozmiaru danych: `readString` wczytuje całość do pamięci.
- `Path` nie jest walidatorem bezpieczeństwa. Ten lab używa stałych nazw
  plików; nie wystawiaj dowolnych ścieżek od użytkownika jako publicznego API.
- Zwykłe `writeString` nie gwarantuje atomowej podmiany pliku ani
  transakcyjności. Nie traktuj go jako trwałego dziennika operacji bankowych.

## Pytania kontrolne

1. Czy `resolve(...)` tworzy plik?
2. Jak zachowuje się `writeString` przy istniejącym pliku?
3. Czy zapis tworzy brakujący katalog nadrzędny?
4. Dlaczego test sprawdza bajty UTF-8, a nie tylko odczytany tekst?
5. Czy liczba znaków zawsze jest równa rozmiarowi pliku w bajtach?
6. Co zapewnia pole `@TempDir` bez `@Shared`?
7. Dlaczego brak pliku nie powinien być zamieniany na pusty string?
8. Kiedy `readString` nie jest właściwym wyborem?

Do zapamiętania:

```text
Path.resolve       → buduje ścieżkę, nie tworzy pliku
Files.writeString  → zapisuje tekst; domyślnie zastępuje zawartość
Files.readString   → odczytuje cały tekst do pamięci
UTF-8              → jawny kontrakt kodowania
@TempDir           → izolowany katalog testowy zarządzany przez Spocka
błąd I/O           → wyjątek, nie pusty raport ani pozorny sukces
```
