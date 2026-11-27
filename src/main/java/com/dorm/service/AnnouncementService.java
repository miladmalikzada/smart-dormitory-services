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

    public Announcement get(String id) {
        return announcements.findById(id).orElseThrow(() -> new NotFoundException("Unknown announcement " + id));
    }

    public Announcement update(String id, String title, String body) {
        Announcement old = get(id);
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title required");
        Announcement a = new Announcement(id, title, body, old.postedAt());
        announcements.save(a);
        events.publish(new DormEvent("ANNOUNCEMENT_UPDATED", "Announcement updated: " + title));
        return a;
    }

    public void delete(String id) {
        get(id);
        announcements.delete(id);
        events.publish(new DormEvent("ANNOUNCEMENT_DELETED", "Announcement " + id + " deleted"));
    }

    public List<Announcement> list() { return announcements.findAll(); }
}
