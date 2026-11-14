package com.delta.bank.lab.patterns.creational;

import java.util.concurrent.atomic.AtomicLong;

public enum IdGenerator {
    INSTANCE;

    private final AtomicLong counter = new AtomicLong();

    public String next(String prefix) {
        return String.format("%s-%d", prefix, counter.incrementAndGet());
    }
}