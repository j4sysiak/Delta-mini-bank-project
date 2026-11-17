package com.delta.bank.lab.patterns.behavioral2.command;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;


// Command opakowuje żądanie jako obiekt; kolejka (invoker) może je odroczyć.
public final class CommandQueue {

    // To pole przechowuje kolejkę poleceń typu `BankCommand`.
    // `ArrayDeque` realizuje kolejkę FIFO, więc polecenia można wykonywać w kolejności ich dodania.
    // `private` ogranicza dostęp do pola z zewnątrz klasy, a `final` oznacza, że pole nie może wskazywać na inną kolejkę.
    private final Queue<BankCommand> commands = new ArrayDeque<>();

    public void submit(BankCommand command) {
        commands.add(Objects.requireNonNull(command, "command"));
    }

    public boolean runNext() {
        BankCommand command = commands.poll();
        if (command == null) {
            return false;
        }
        command.execute();
        return true;
    }

    public void runAll() {
        while (runNext()) {
            // Execute queued commands in submission order.
        }
    }

    public int size() {
        return commands.size();
    }
}