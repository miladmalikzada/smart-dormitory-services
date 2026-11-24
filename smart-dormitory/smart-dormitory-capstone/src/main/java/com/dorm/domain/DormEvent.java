package com.dorm.domain;

/** Event sent through the Observer mechanism. */
public record DormEvent(String type, String message) { }
