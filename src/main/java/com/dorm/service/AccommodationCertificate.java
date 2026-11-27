package com.dorm.service;

import java.time.LocalDate;

/** The facts printed on an accommodation certificate. It never contains identity numbers. */
public record AccommodationCertificate(String studentId, String studentName, String faculty, Integer courseYear,
                                       String roomId, LocalDate issuedOn) { }
