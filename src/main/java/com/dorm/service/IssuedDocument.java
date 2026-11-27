package com.dorm.service;

/** A finished document ready to be sent to the browser or saved. */
public record IssuedDocument(String filename, String contentType, String content) { }
