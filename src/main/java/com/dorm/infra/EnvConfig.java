package com.dorm.infra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Reads secrets from a .env file (KEY=value lines). Real environment variables win over the file. */
public final class EnvConfig {
    private final Map<String, String> values;

    private EnvConfig(Map<String, String> values) { this.values = values; }

    public static EnvConfig of(Map<String, String> values) { return new EnvConfig(new HashMap<>(values)); }

    public static EnvConfig load(Path file) {
        Map<String, String> m = new HashMap<>();
        if (Files.isRegularFile(file)) {
            try {
                for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq <= 0) continue;
                    String value = line.substring(eq + 1).trim();
                    if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                            || value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    m.put(line.substring(0, eq).trim(), value);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + file + ": " + e.getMessage(), e);
            }
        }
        for (Map.Entry<String, String> e : System.getenv().entrySet()) {
            if (e.getKey().startsWith("DORM_")) m.put(e.getKey(), e.getValue());
        }
        return new EnvConfig(m);
    }

    public String get(String key) {
        String v = values.get(key);
        return v == null || v.isBlank() ? null : v;
    }

    public String require(String key) {
        String v = get(key);
        if (v == null) throw new IllegalStateException("Missing " + key + ". Run: python3 scripts/setup_env.py (creates the .env file)");
        return v;
    }
}
