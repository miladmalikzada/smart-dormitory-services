package com.dorm.service;

import java.time.Clock;

/**
 * Factory Method pattern (L3). Subclasses decide which channel to build;
 * the base class guarantees that every channel is wrapped by the same Decorator.
 */
public abstract class ChannelFactory {
    private final Clock clock;

    protected ChannelFactory(Clock clock) { this.clock = clock; }

    protected abstract NotificationChannel createChannel();

    public final NotificationChannel create() {
        return new TimestampedChannel(createChannel(), clock);
    }
}
