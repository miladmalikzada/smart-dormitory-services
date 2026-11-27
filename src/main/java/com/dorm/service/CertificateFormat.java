package com.dorm.service;

/** Strategy (L6) for the output format of a certificate. A new format = one new class plus one line in App. */
public interface CertificateFormat {
    /** Short name used in the URL, e.g. "html". */
    String id();
    String contentType();
    String extension();
    String render(AccommodationCertificate certificate);
}
