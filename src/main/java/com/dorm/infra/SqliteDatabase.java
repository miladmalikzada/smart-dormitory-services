package com.dorm.infra;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/** Opens the SQLite file and creates the schema from schema.sql (ER diagram: docs/DATABASE.md). */
public final class SqliteDatabase {
    private SqliteDatabase() { }

    public static Connection open(Path file) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Connection c = DriverManager.getConnection("jdbc:sqlite:" + file);
            createSchema(c);
            return c;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot open database " + file + ": " + e.getMessage(), e);
        }
    }

    /** Single source of truth: the same schema.sql file that is committed in the repository. */
    static void createSchema(Connection c) throws SQLException, IOException {
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        String sql;
        try (InputStream in = SqliteDatabase.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new IOException("schema.sql not found on the classpath");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        ensureCurrentSchema(c);
        String withoutComments = sql.lines().filter(l -> !l.trim().startsWith("--")).collect(Collectors.joining("\n"));
        for (String statement : withoutComments.split(";")) {
            if (statement.isBlank()) continue;
            try (Statement st = c.createStatement()) {
                st.execute(statement);
            }
        }
    }

    /** An old data/dorm.db from before the student-details upgrade has a different students table. */
    private static void ensureCurrentSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT 1 FROM pragma_table_info('students') WHERE name = 'name'")) {
            if (rs.next()) {
                throw new SQLException("This database uses the old students table. Delete the data/ folder "
                        + "(it only holds demo data) and start again.");
            }
        }
    }
}
