package com.dorm.service;

import java.util.List;

public interface CertificateOperations {
    /** Certificate for a student who lives in a room. */
    IssuedDocument issue(String studentId, String formatId);

    /** The format ids that can be requested (text, html, csv...). */
    List<String> formats();
}
