package com.delta.bank.lab.patterns.creational.prototype;

// Interfejs określa, że obiekt potrafi zwrócić swoją kopię.
// Typ parametru sprawia, że wynik copy() ma konkretny typ, bez rzutowania.

public interface Prototype<T> {
    T copy();
}