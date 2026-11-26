package com.dorm.infra;

import com.dorm.service.NotificationChannel;

import java.io.PrintStream;

public class ConsoleChannel implements NotificationChannel {
    private final PrintStream out;

    public ConsoleChannel(PrintStream out) { this.out = out; }

    @Override public String name() { return "console"; }

    @Override public void send(String recipient, String message) {
        out.println("  [notice -> " + recipient + "] " + message);
    }
}
