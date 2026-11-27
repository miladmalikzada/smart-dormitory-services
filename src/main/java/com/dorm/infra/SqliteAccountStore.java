package com.dorm.infra;

import com.dorm.domain.AccountStatus;
import com.dorm.domain.Role;
import com.dorm.service.AccountRecord;
import com.dorm.service.AccountStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/** Accounts in the SQLite table "accounts" (salt and PBKDF2 hash as Base64 text). */
public class SqliteAccountStore implements AccountStore {
    private final Connection db;

    public SqliteAccountStore(Connection db) { this.db = db; }

    @Override public Optional<AccountRecord> find(String username) {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM accounts WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet r = ps.executeQuery()) { return r.next() ? Optional.of(map(r)) : Optional.empty(); }
        } catch (SQLException e) { throw new IllegalStateException("Database error: " + e.getMessage(), e); }
    }

    @Override public void save(AccountRecord a) {
        try (PreparedStatement ps = db.prepareStatement(
                "INSERT OR REPLACE INTO accounts(username, salt, password_hash, role, status, created_at) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, a.username());
            ps.setString(2, Base64.getEncoder().encodeToString(a.salt()));
            ps.setString(3, Base64.getEncoder().encodeToString(a.hash()));
            ps.setString(4, a.role().name());
            ps.setString(5, a.status().name());
            ps.setString(6, a.createdAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Database error: " + e.getMessage(), e); }
    }

    @Override public List<AccountRecord> all() {
        List<AccountRecord> out = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM accounts ORDER BY created_at");
             ResultSet r = ps.executeQuery()) {
            while (r.next()) out.add(map(r));
        } catch (SQLException e) { throw new IllegalStateException("Database error: " + e.getMessage(), e); }
        return out;
    }

    private static AccountRecord map(ResultSet r) throws SQLException {
        return new AccountRecord(r.getString("username"),
                Base64.getDecoder().decode(r.getString("salt")), Base64.getDecoder().decode(r.getString("password_hash")),
                Role.valueOf(r.getString("role")), AccountStatus.valueOf(r.getString("status")),
                Instant.parse(r.getString("created_at")));
    }
}
