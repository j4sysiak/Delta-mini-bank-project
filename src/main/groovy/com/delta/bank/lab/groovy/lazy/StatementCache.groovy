package com.delta.bank.lab.groovy.lazy

class StatementCache {

    int loads = 0

    @Lazy
    List<String> lines = load()

    private List<String> load() {
        loads++
        ["line-1", "line-2"]
    }
}