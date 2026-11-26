package com.dorm.service;

/** Extension point: a new delivery channel is a new class implementing this interface. */
public interface NotificationChannel {
    String name();
    void send(String recipient, String message);
}
