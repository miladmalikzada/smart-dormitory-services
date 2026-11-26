package com.dorm.service;

import com.dorm.domain.Announcement;
import com.dorm.domain.DormEvent;
import com.dorm.domain.Repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

public class AnnouncementService {
    private final Repository<Announcement> announcements;
    private final EventPublisher events;
    private final Clock clock;

    public AnnouncementService(Repository<Announcement> announcements, EventPublisher events, Clock clock) {
        this.announcements = announcements;
        this.events = events;
        this.clock = clock;
    }

    public Announcement post(String title, String body) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title required");
        Announcement a = new Announcement(Ids.next("A"), title, body, LocalDateTime.now(clock));
        announcements.save(a);
        events.publish(new DormEvent("ANNOUNCEMENT_POSTED", title + ": " + body));
        return a;
    }

    public List<Announcement> list() { return announcements.findAll(); }
}
