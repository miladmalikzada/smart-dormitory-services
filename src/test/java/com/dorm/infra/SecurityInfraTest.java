package com.dorm.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecurityInfraTest {
    @TempDir Path tmp;

    @Test void encryptionRoundTripsAndHidesTheValue() {
        AesGcmCipher c = AesGcmCipher.fromBase64(AesGcmCipher.generateKey());
        String enc = c.encrypt("040517501237");
        assertNotEquals("040517501237", enc);
        assertFalse(enc.contains("0405175"));
        assertEquals("040517501237", c.decrypt(enc));
        assertNull(c.encrypt(null));
        assertNull(c.decrypt(null));
    }

    @Test void everyEncryptionUsesAFreshIv() {
        AesGcmCipher c = AesGcmCipher.fromBase64(AesGcmCipher.generateKey());
        assertNotEquals(c.encrypt("same"), c.encrypt("same"));
    }

    @Test void wrongKeyOrTamperedDataIsRejected() {
        AesGcmCipher a = AesGcmCipher.fromBase64(AesGcmCipher.generateKey());
        AesGcmCipher b = AesGcmCipher.fromBase64(AesGcmCipher.generateKey());
        String enc = a.encrypt("secret");
        assertThrows(IllegalStateException.class, () -> b.decrypt(enc));
        char[] chars = enc.toCharArray();
        chars[chars.length - 6] = chars[chars.length - 6] == 'A' ? 'B' : 'A';
        assertThrows(IllegalStateException.class, () -> a.decrypt(new String(chars)));
    }

    @Test void keyMustBe32Bytes() {
        assertThrows(IllegalArgumentException.class, () -> AesGcmCipher.fromBase64("c2hvcnQ="));
        assertThrows(IllegalArgumentException.class, () -> AesGcmCipher.fromBase64("not base64!!"));
        assertThrows(IllegalArgumentException.class, () -> AesGcmCipher.fromBase64(null));
    }

    @Test void envFileIsParsed() throws Exception {
        Path f = tmp.resolve(".env");
        Files.writeString(f, "# comment\n\nDORM_A=one\nDORM_B = \"two words\"\nDORM_C='three'\nbroken line\nDORM_EMPTY=\n");
        EnvConfig cfg = EnvConfig.load(f);
        assertEquals("one", cfg.get("DORM_A"));
        assertEquals("two words", cfg.get("DORM_B"));
        assertEquals("three", cfg.get("DORM_C"));
        assertNull(cfg.get("DORM_EMPTY"));
        assertNull(cfg.get("MISSING"));
        assertEquals("one", cfg.require("DORM_A"));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> cfg.require("MISSING"));
        assertTrue(e.getMessage().contains("setup_env.py"));
    }

    @Test void missingEnvFileGivesEmptyConfig() {
        assertNull(EnvConfig.load(tmp.resolve("nope.env")).get("DORM_ADMIN_PASSWORD"));
        assertNull(EnvConfig.of(Map.of()).get("X"));
    }
}
