package com.dorm.service;

import java.util.UUID;

final class Ids {
    private Ids() { }
    static String next(String prefix) { return prefix + "-" + UUID.randomUUID().toString().substring(0, 6); }
}
