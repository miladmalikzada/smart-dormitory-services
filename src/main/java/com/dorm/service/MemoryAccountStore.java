package com.dorm.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Accounts kept in memory only (used with --storage=memory or file, and in tests). */
public class MemoryAccountStore implements AccountStore {
    private final Map<String, AccountRecord> accounts = new LinkedHashMap<>();

    @Override public synchronized Optional<AccountRecord> find(String username) { return Optional.ofNullable(accounts.get(username)); }
    @Override public synchronized void save(AccountRecord account) { accounts.put(account.username(), account); }
    @Override public synchronized List<AccountRecord> all() { return new ArrayList<>(accounts.values()); }
}
