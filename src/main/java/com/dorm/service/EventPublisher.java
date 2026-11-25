package com.dorm.service;

import com.dorm.domain.DormEvent;
import com.dorm.domain.DormEventListener;

import java.util.ArrayList;
import java.util.List;

/** Observer pattern (L5): the Subject. Services publish events and never know who listens. */
public class EventPublisher {
    private final List<DormEventListener> listeners = new ArrayList<>();

    public void subscribe(DormEventListener listener) { listeners.add(listener); }

    public void publish(DormEvent event) {
        for (DormEventListener l : List.copyOf(listeners)) l.onEvent(event);
    }
}
