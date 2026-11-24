package com.dorm.service;

import com.dorm.domain.DormEvent;
import com.dorm.domain.DormEventListener;

import java.util.List;
import java.util.Set;

/** Observer: forwards selected events to every registered channel. */
public class NotificationListener implements DormEventListener {
    private final List<NotificationChannel> channels;
    private final Set<String> notifyOn;

    public NotificationListener(List<NotificationChannel> channels, Set<String> notifyOn) {
        this.channels = List.copyOf(channels);
        this.notifyOn = Set.copyOf(notifyOn);
    }

    @Override public void onEvent(DormEvent event) {
        if (!notifyOn.contains(event.type())) return;
        for (NotificationChannel c : channels) c.send("residents", event.message());
    }
}
