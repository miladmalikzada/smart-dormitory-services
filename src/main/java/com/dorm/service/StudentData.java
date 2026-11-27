package com.dorm.service;

/** Input for creating or updating a student (all text; the service validates it). */
public record StudentData(String firstName, String lastName, String dateOfBirth, String gender, String nationality,
                          String iin, String nationalId, String passportNumber, String phone, String email,
                          String faculty, Integer courseYear, String address, String emergencyName,
                          String emergencyPhone) { }
