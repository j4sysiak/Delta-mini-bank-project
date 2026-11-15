package com.delta.bank.lab.patterns.behavioral2.command;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;


// Command opakowuje żądanie jako obiekt; kolejka (invoker) może je odroczyć.
public final class CommandQueue {

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