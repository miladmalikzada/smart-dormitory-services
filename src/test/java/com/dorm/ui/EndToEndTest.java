package com.dorm.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.App;
import com.dorm.infra.EnvConfig;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EndToEndTest {
    private static final String KEY = com.dorm.infra.AesGcmCipher.generateKey();
    @TempDir Path tmp;

    private String run(String storage, String script) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        App.build(storage, tmp, new ByteArrayInputStream(script.getBytes()), new PrintStream(buf), EnvConfig.of(Map.of("DORM_ENCRYPTION_KEY", KEY))).run();
        return buf.toString();
    }

    @Test void mainUseCaseWorksInMemory() {
        String out = run("memory", String.join("\n",
                "1", "Ayan", "777",     // register student
                "4",                    // list rooms
                "13", "Water", "Off tonight",
                "15", "STAFF",
                "0")) + "\n";
        assertTrue(out.contains("Registered S-"));
        assertTrue(out.contains("R101"));
        assertTrue(out.contains("[SMS gateway]"));     // announcement fanned out to both channels
        assertTrue(out.contains("[notice -> residents]"));
        assertTrue(out.contains("Goodbye."));
    }

    @Test void studentSeesAccessDeniedForTicketWork() {
        String out = run("memory", "6\nT-1\nBob\n0\n");
        assertTrue(out.contains("Access denied"));
    }

    @Test void fileStorageSurvivesARestart() {
        run("file", "13\nHello\nWorld\n0\n");
        String out = run("file", "14\n0\n");
        assertTrue(out.contains("Hello - World"));
    }

    @Test void sqliteStorageSurvivesARestart() {
        run("sqlite", "13\nHello\nWorld\n0\n");
        assertTrue(run("sqlite", "14\n0\n").contains("Hello - World"));
    }

    @Test void badInputIsReportedNotThrown() {
        assertTrue(run("memory", "2\nnobody\n0\n").contains("Error:"));
        assertTrue(run("memory", "99\n0\n").contains("Unknown option"));
    }
}
