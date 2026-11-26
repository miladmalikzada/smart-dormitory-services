package com.dorm.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Invoker of the Command pattern: runs commands and remembers them for undo. */
public class CommandHistory {
    private final Deque<Command> done = new ArrayDeque<>();

    public void execute(Command command) {
        command.execute();
        done.push(command);
    }

    /** @return the description of the undone command, or empty if there is nothing to undo. */
    public Optional<String> undoLast() {
        if (done.isEmpty()) return Optional.empty();
        Command c = done.pop();
        c.undo();
        return Optional.of(c.description());
    }

    public int size() { return done.size(); }
}
