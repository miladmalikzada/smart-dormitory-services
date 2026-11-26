package com.dorm.service;

import java.time.Clock;
import java.time.LocalDateTime;

/** Decorator pattern (L4): adds a timestamp to any channel without changing the channel. */
public class TimestampedChannel implements NotificationChannel {
    private final NotificationChannel inner;
    private final Clock clock;

    public TimestampedChannel(NotificationChannel inner, Clock clock) {
        this.inner = inner;
        this.clock = clock;
    }

    @Override public String name() { return inner.name(); }

    @Override public void send(String recipient, String message) {
        inner.send(recipient, "(" + LocalDateTime.now(clock).withNano(0) + ") " + message);
    }
}
