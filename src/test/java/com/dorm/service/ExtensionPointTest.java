package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.DormEvent;
import com.dorm.infra.InMemoryRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Open/Closed proof: PushChannel and PushChannelFactory exist only in this test.
 * A new delivery channel needs no change to any production class except the composition root line.
 */
class ExtensionPointTest {
    static class PushChannel implements NotificationChannel {
        final List<String> pushed = new ArrayList<>();
        public String name() { return "push"; }
        public void send(String recipient, String message) { pushed.add(recipient + ":" + message); }
    }

    static class PushChannelFactory extends ChannelFactory {
        final PushChannel channel = new PushChannel();
        PushChannelFactory() { super(Clock.systemUTC()); }
        protected NotificationChannel createChannel() { return channel; }
    }

    @Test void newChannelWorksWithoutEditingExistingClasses() {
        PushChannelFactory factory = new PushChannelFactory();
        EventPublisher events = new EventPublisher();
        events.subscribe(new NotificationListener(List.of(factory.create()), Set.of("X")));
        events.publish(new DormEvent("X", "fire drill"));
        assertEquals(1, factory.channel.pushed.size());
        assertTrue(factory.channel.pushed.get(0).contains("fire drill"));
    }

    @Test void floorStrategyPlugsIntoRoomServiceWithoutChangingIt() {
        RoomService service = new RoomService(new InMemoryRepository<>(), new InMemoryRepository<>(),
                new FloorStrategy(), new CommandHistory(), new EventPublisher());
        service.addRoom("R201", 2);
        service.addRoom("R101", 1);
        assertEquals("R101", service.requestRoom(service.registerStudent("Ayan", "1").id()).id());
        assertEquals("R201", service.requestRoom(service.registerStudent("Dana", "2").id()).id());
    }
}
