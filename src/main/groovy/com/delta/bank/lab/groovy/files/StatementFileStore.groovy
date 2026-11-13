package com.delta.bank.lab.groovy.files

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

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