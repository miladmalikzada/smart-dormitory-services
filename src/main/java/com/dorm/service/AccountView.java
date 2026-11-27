package com.dorm.service;

import com.dorm.domain.AccountStatus;
import com.dorm.domain.Role;
import java.time.Instant;

/** What may be shown about an account: no salt, no hash. */
public record AccountView(String username, Role role, AccountStatus status, Instant createdAt) {
    static AccountView of(AccountRecord r) { return new AccountView(r.username(), r.role(), r.status(), r.createdAt()); }
}
