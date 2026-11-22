package com.delta.bank.lab.patterns.creational.flyweight;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Fabryka obiektów lekkich reprezentujących wspólny styl transakcji.
 *
 * <p>Współdzielone są instancje {@link TransactionStyle} dla tego samego typu transakcji, reprezentowanego przez enum
 * {@link TransactionType}. Klasa jest finalna, ponieważ nie przewiduje się jej dziedziczenia.</p>
 */
public final class TransactionStyleFactory {

    private final ConcurrentMap<TransactionType, TransactionStyle> cache =
            new ConcurrentHashMap<>();

    /**
     * Zwraca współdzielony styl dla typu transakcji, tworząc go tylko przy pierwszym użyciu.
     */
    public TransactionStyle styleFor(TransactionType type) {
        Objects.requireNonNull(type, "type");
        return cache.computeIfAbsent(type, TransactionStyleFactory::createStyle);
    }

    /*
    @FunctionalInterface
    public interface Function<T, R> {
      R apply(T t);
    }

    Function fun = new Function() {
        @Override
        public R apply(T t) {
            return createStyle(t);
        }
    };

    Lambda expression:
    Function fun = (T t) -> createStyle(t);

    Reference to a static method:
    Function fun = TransactionStyleFactory::createStyle;

    */


    /**
     * Tworzy nowy styl dla danego typu transakcji.
     */
    private static TransactionStyle createStyle(TransactionType type) {
        return switch (type) {
            case DEPOSIT -> new TransactionStyle("Deposit", "+");
            case WITHDRAWAL -> new TransactionStyle("Withdrawal", "-");
            case TRANSFER -> new TransactionStyle("Transfer", ">");
        };
    }
}