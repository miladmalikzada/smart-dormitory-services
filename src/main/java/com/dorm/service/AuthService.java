package com.dorm.service;

import com.dorm.domain.Role;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Login with salted PBKDF2 password hashes, random Bearer tokens that expire, and a lockout
 * after too many wrong passwords. Passwords come from the .env file, never from the code.
 */
public class AuthService {
    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_WINDOW = Duration.ofSeconds(60);
    public static final Duration TOKEN_LIFETIME = Duration.ofHours(8);
    private static final int ITERATIONS = 120_000;

    public record Login(String token, String username, Role role, Instant expiresAt) { }
    private record Account(byte[] salt, byte[] hash, Role role) { }
    private record Session(String username, Role role, Instant expiresAt) { }

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, Deque<Instant>> failures = new HashMap<>();

    public AuthService(Clock clock) { this.clock = clock; }

    public synchronized void addAccount(String username, String password, Role role) {
        if (password == null || password.length() < 8) throw new IllegalArgumentException("password for " + username + " must have 8+ characters");
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        accounts.put(username, new Account(salt, hash(password, salt), role));
    }

    public synchronized boolean hasAccounts() { return !accounts.isEmpty(); }

    public synchronized Login login(String username, String password) {
        String user = username == null ? "" : username.trim().toLowerCase();
        Instant now = clock.instant();
        Deque<Instant> recent = failures.computeIfAbsent(user, k -> new ArrayDeque<>());
        while (!recent.isEmpty() && recent.peekFirst().isBefore(now.minus(LOCK_WINDOW))) recent.pollFirst();
        if (recent.size() >= MAX_FAILURES) throw new RateLimitedException("Too many failed attempts. Try again in a minute.");

        Account a = accounts.get(user);
        // Always hash, even for unknown users, so response time does not reveal which usernames exist.
        byte[] salt = a == null ? new byte[16] : a.salt();
        byte[] candidate = hash(password == null ? "" : password, salt);
        boolean ok = a != null && MessageDigest.isEqual(candidate, a.hash());
        if (!ok) {
            recent.addLast(now);
            throw new AuthenticationException("Invalid username or password");
        }
        recent.clear();
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Instant expires = now.plus(TOKEN_LIFETIME);
        sessions.put(token, new Session(user, a.role(), expires));
        return new Login(token, user, a.role(), expires);
    }

    /** @return the session's login data, or empty when the token is unknown or expired */
    public synchronized Optional<Login> authenticate(String token) {
        if (token == null) return Optional.empty();
        Session s = sessions.get(token);
        if (s == null) return Optional.empty();
        if (!s.expiresAt().isAfter(clock.instant())) { sessions.remove(token); return Optional.empty(); }
        return Optional.of(new Login(token, s.username(), s.role(), s.expiresAt()));
    }

    public synchronized void logout(String token) { if (token != null) sessions.remove(token); }

    private static byte[] hash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is not available", e);
        }
    }
}
