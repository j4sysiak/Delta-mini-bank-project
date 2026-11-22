package com.delta.bank.lab.patterns.creational.flyweight;

import java.util.List;
import java.util.Objects;

/**
 * Renderuje pojedyncze transakcje lub ich listę do napisów.
 *
 * <p>Styl dla każdej transakcji pobiera z {@link TransactionStyleFactory},
 * dzięki czemu współdzieli obiekty stylów zamiast tworzyć osobny styl
 * dla każdej pozycji.</p>
 */
public final class StatementRenderer {

    private final TransactionStyleFactory styles;

    public StatementRenderer(TransactionStyleFactory styles) {
        this.styles = Objects.requireNonNull(styles, "styles");
    }

    public String render(TransactionEntry entry) {
        Objects.requireNonNull(entry, "entry");
        TransactionStyle style = styles.styleFor(entry.type());
        return style.render(entry.description(), entry.amount().toPlainString());
    }

    // renderAll tworzy wynikowe napisy, ale nie tworzy nowego stylu dla każdej pozycji.
    public List<String> renderAll(List<TransactionEntry> entries) {
        Objects.requireNonNull(entries, "entries");
        return List.copyOf(entries).stream()
                .map(this::render)
                .toList();
    }
}