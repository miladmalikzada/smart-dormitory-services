package com.dorm.service;

/** Thrown when an id does not exist. The web layer turns it into HTTP 404. */
public class NotFoundException extends IllegalArgumentException {
    public NotFoundException(String message) { super(message); }
}
