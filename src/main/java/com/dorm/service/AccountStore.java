package com.dorm.service;

import java.util.List;
import java.util.Optional;

/** Where accounts are kept. The service depends on this interface; SQLite (infra) or memory provide it. */
public interface AccountStore {
    Optional<AccountRecord> find(String username);
    /** Inserts the account or replaces the one with the same username. */
    void save(AccountRecord account);
    List<AccountRecord> all();
}
