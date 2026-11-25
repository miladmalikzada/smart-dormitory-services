package com.dorm.infra;

import com.dorm.domain.Entity;
import com.dorm.domain.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Template Method (L6) for the SQL repositories: this class owns the shared query flow,
 * subclasses supply the table name, the row mapping and the insert/update.
 */
abstract class SqlRepository<T extends Entity> implements Repository<T> {
    protected final Connection db;

    protected SqlRepository(Connection db) { this.db = db; }

    protected abstract String table();
    protected abstract T map(ResultSet row) throws SQLException;
    protected abstract void upsert(T entity) throws SQLException;

    @Override public void save(T entity) {
        try { upsert(entity); } catch (SQLException e) { throw fail(e); }
    }

    @Override public Optional<T> findById(String id) {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM " + table() + " WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw fail(e); }
    }

    @Override public List<T> findAll() {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM " + table() + " ORDER BY rowid");
             ResultSet rs = ps.executeQuery()) {
            List<T> all = new ArrayList<>();
            while (rs.next()) all.add(map(rs));
            return all;
        } catch (SQLException e) { throw fail(e); }
    }

    @Override public void delete(String id) {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM " + table() + " WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw fail(e); }
    }

    private IllegalStateException fail(SQLException e) {
        return new IllegalStateException("Database error: " + e.getMessage(), e);
    }
}
