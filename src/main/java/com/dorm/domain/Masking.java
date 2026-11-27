package com.dorm.domain;

/** Hides all but the last four characters of a sensitive value (staff never need the full number). */
public final class Masking {
    private Masking() { }

    public static String mask(String value) {
        if (value == null || value.isEmpty()) return value;
        if (value.length() <= 4) return "****";
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
