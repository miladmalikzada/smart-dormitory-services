package com.dorm.service;

import com.dorm.domain.AccountStatus;
import com.dorm.domain.Role;
import java.time.Instant;

/** One stored account. Only a salted hash of the password is kept, never the password. */
public record AccountRecord(String username, byte[] salt, byte[] hash, Role role, AccountStatus status, Instant createdAt) {
    public AccountRecord withStatus(AccountStatus newStatus) {
        return new AccountRecord(username, salt, hash, role, newStatus, createdAt);
    }
}
