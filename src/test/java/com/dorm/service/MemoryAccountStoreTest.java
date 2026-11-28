package com.dorm.service;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.AccountStatus;
import com.dorm.domain.Role;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemoryAccountStoreTest {
    private MemoryAccountStore store;

    private static AccountRecord account(String username) {
        return new AccountRecord(username, new byte[] {1}, new byte[] {2}, Role.STUDENT, AccountStatus.ACTIVE,
                Instant.parse("2026-11-26T10:00:00Z"));
    }

    @BeforeEach void setUp() { store = new MemoryAccountStore(); }

    @Test void aSavedAccountCanBeFoundAndAnUnknownOneCannot() {
        store.save(account("aida"));
        assertEquals("aida", store.find("aida").orElseThrow().username());
        assertTrue(store.find("ghost").isEmpty());
    }
}
