package com.delta.bank.lab.patterns.creational.prototype;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TransferDraft implements Prototype<TransferDraft> {

    private final String from;
    private final String to;
    private final BigDecimal amount;
    private final List<String> tags;

    // Ten publiczny konstruktor kopiuje przekazaną listę, więc późniejsze zmiany listy wejściowej nie zmienią obiektu.
    public TransferDraft(String from, String to, BigDecimal amount, List<String> tags) {
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.amount = Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(tags, "tags");

        if (from.isBlank() || to.isBlank()) {
            throw new IllegalArgumentException("account must not be blank");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("accounts must be different");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        this.tags = new ArrayList<>(List.copyOf(tags));
    }

    // Konstruktor kopiujący tworzy nową listę tags, czyli copy() nie współdzieli mutowalnego stanu z oryginałem.
    // BigDecimal i String są niemutowalne, dlatego można bezpiecznie skopiować ich referencje.
    // Dla większej precyzji można napisać: „copy() tworzy niezależną listę tagów;
    //   jej elementy oraz pozostałe pola są niemutowalne, więc ich referencje mogą być współdzielone.”
    private TransferDraft(TransferDraft source) {
        this.from = source.from;  // niemutowalne
        this.to = source.to;      // niemutowalne
        this.amount = source.amount;  // niemutowalne
        this.tags = new ArrayList<>(source.tags); // tworzy nową mutowalną listę, więc copy() nie współdzieli stanu z oryginałem
    }

    @Override
    public TransferDraft copy() {
        return new TransferDraft(this);
    }

    public void addTag(String tag) {
        Objects.requireNonNull(tag, "tag");
        if (tag.isBlank()) {
            throw new IllegalArgumentException("tag must not be blank");
        }
        tags.add(tag);
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public BigDecimal amount() {
        return amount;
    }

    public List<String> tags() {
        return List.copyOf(tags);
    }
}