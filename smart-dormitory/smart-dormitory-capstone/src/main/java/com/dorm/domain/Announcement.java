package com.dorm.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

public record Announcement(String id, String title, String body, LocalDateTime postedAt)
        implements Entity, Serializable { }
