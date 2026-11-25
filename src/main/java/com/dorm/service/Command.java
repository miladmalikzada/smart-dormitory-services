package com.dorm.service;

/** Command pattern (L5): an operation that can be executed and undone. */
public interface Command {
    void execute();
    void undo();
    String description();
}
