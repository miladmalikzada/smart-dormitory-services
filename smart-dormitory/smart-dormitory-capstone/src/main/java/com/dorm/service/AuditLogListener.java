package com.dorm.service;

import com.dorm.domain.DormEvent;
import com.dorm.domain.DormEventListener;

import java.util.ArrayList;
import java.util.List;

/** Observer: keeps every event, so the operation log needs no change in the services. */
public class AuditLogListener implements DormEventListener {
    private final List<DormEvent> events = new ArrayList<>();

    @Override public void onEvent(DormEvent event) { events.add(event); }

    public List<DormEvent> events() { return List.copyOf(events); }
}
