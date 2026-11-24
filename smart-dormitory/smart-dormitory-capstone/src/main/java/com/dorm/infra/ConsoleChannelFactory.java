package com.dorm.infra;

import com.dorm.service.ChannelFactory;
import com.dorm.service.NotificationChannel;

import java.io.PrintStream;
import java.time.Clock;

public class ConsoleChannelFactory extends ChannelFactory {
    private final PrintStream out;

    public ConsoleChannelFactory(Clock clock, PrintStream out) {
        super(clock);
        this.out = out;
    }

    @Override protected NotificationChannel createChannel() { return new ConsoleChannel(out); }
}
