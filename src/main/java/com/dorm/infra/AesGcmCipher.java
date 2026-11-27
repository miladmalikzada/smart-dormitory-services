package com.dorm.infra;

import com.dorm.service.FieldCipher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-256-GCM: every value gets its own random 12-byte IV; stored as base64(iv + ciphertext + tag). */
public final class AesGcmCipher implements FieldCipher {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    private AesGcmCipher(byte[] keyBytes) { this.key = new SecretKeySpec(keyBytes, "AES"); }

    public static AesGcmCipher fromBase64(String base64Key) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(base64Key == null ? "" : base64Key.trim()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("DORM_ENCRYPTION_KEY is not valid base64"); }
        if (bytes.length != 32) throw new IllegalArgumentException("DORM_ENCRYPTION_KEY must be 32 bytes (base64). Run: python3 scripts/setup_env.py");
        return new AesGcmCipher(bytes);
    }

    public static String generateKey() {
        byte[] k = new byte[32];
        new SecureRandom().nextBytes(k);
        return Base64.getEncoder().encodeToString(k);
    }

    @Override public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override public String decrypt(String stored) {
        if (stored == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(stored);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, all, 0, 12));
            return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot decrypt a stored value (wrong DORM_ENCRYPTION_KEY?)", e);
        }
    }
}
