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
        // Ta linia tworzy obiekt Path wskazujący na plik statement.txt w katalogu tymczasowym testu (tempDir).
        // Sama nie tworzy pliku — plik powstaje później, gdy store.write(path, content) zapisze do niego treść.
        Path path = tempDir.resolve("statement.txt")
        String content = "Account: PLN-1001\nBalance: 125.50 PLN\n"

        when:
        store.write(path, content) // Zapisuje treść do pliku statement.txt w katalogu tymczasowym.

        then:
        Files.exists(path)
        store.read(path) == content  // Odczytuje treść z pliku statement.txt i porównuje ją z oryginalną treścią.
    }

    def "writes UTF-8 bytes for Polish characters"() {
        given:
        Path path = tempDir.resolve("polish.txt")
        String content = "Wpłata: Łódź"

        when:
        store.write(path, content) // Zapisuje treść z polskimi znakami do pliku polish.txt w katalogu tymczasowym.

        then:
        /*
        Arrays.equals(...) porównuje zawartość dwóch tablic bajtów:
         - Files.readAllBytes(path) — bajty zapisane w pliku,
         - content.getBytes(StandardCharsets.UTF_8) — bajty, które powinny powstać z tekstu zakodowanego w UTF‑8.

        Test sprawdza więc, czy plik zawiera dokładnie oczekiwane bajty,
          a nie tylko tekst, który po odczytaniu mógłby przypadkiem wyglądać poprawnie przy innym kodowaniu.
        * */
        Arrays.equals(
                Files.readAllBytes(path),
                content.getBytes(StandardCharsets.UTF_8)
        )
        store.read(path) == content  // Odczytuje treść z pliku polish.txt i porównuje ją z oryginalną treścią, aby upewnić się, że polskie znaki zostały zapisane i odczytane poprawnie.
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