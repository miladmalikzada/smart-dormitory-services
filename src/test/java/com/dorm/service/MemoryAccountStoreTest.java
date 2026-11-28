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

    @Test void savingTheSameUsernameReplacesTheAccount() {
        store.save(account("aida"));
        store.save(account("aida").withStatus(AccountStatus.REJECTED));
        assertEquals(1, store.all().size());
        assertEquals(AccountStatus.REJECTED, store.find("aida").orElseThrow().status());
    }

    @Test void allAccountsComeBackInTheOrderTheyWereSaved() {
        store.save(account("aida"));
        store.save(account("maria"));
        store.save(account("timur"));
        assertEquals(List.of("aida", "maria", "timur"), store.all().stream().map(AccountRecord::username).toList());
    }
}
