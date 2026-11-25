package com.dorm.infra;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.service.NotificationChannel;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AdapterAndDecoratorTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T08:00:00Z"), ZoneOffset.UTC);

    private String capture(java.util.function.Consumer<PrintStream> action) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        action.accept(new PrintStream(buf));
        return buf.toString();
    }

    @Test void adapterTranslatesToTheVendorApiAndTruncates() {
        String out = capture(ps -> new SmsChannelAdapter(new LegacySmsGateway(ps)).send("+7700", "x".repeat(300)));
        assertTrue(out.contains("to=+7700"));
        assertFalse(out.contains("x".repeat(161)));
    }

    @Test void factoryReturnsChannelWrappedByTheTimestampDecorator() {
        String out = capture(ps -> {
            NotificationChannel c = new ConsoleChannelFactory(clock, ps).create();
            c.send("residents", "Hello");
        });
        assertTrue(out.contains("2026-11-24T08:00"));
        assertTrue(out.contains("Hello"));
    }
}
