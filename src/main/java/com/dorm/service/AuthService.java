package com.dorm.service;

import com.dorm.domain.AccountStatus;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Accounts, sign-up and login. Passwords are kept as salted PBKDF2 hashes in an {@link AccountStore}
 * (the SQLite database in production). Anyone may sign up as a student. Staff and admin accounts
 * start as PENDING and cannot sign in until an administrator approves them. Login gives a random
 * Bearer token that expires, and too many wrong passwords lock the username for a minute.
 * The first administrator comes from the .env file (see {@link #addAccount}).
 */
public class AuthService {
    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_WINDOW = Duration.ofSeconds(60);
    public static final Duration TOKEN_LIFETIME = Duration.ofHours(8);
    public static final int MAX_SIGNUPS_PER_MINUTE = 20;
    public static final int MAX_PENDING = 50;
    public static final int TEMPORARY_PASSWORD_LENGTH = 12;
    /** No 0/O and no 1/l/I, so a temporary password can be read out loud or copied by hand without mistakes. */
    private static final String TEMPORARY_ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ITERATIONS = 120_000;
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9][a-z0-9._-]{2,31}");

    public record Login(String token, String username, Role role, Instant expiresAt) { }
    private record Session(String username, Role role, Instant expiresAt) { }

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final AccountStore accounts;
    private final Deque<Instant> signUps = new ArrayDeque<>();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<String, Deque<Instant>> failures = new HashMap<>();

    public AuthService(Clock clock) { this(clock, new MemoryAccountStore()); }

    public AuthService(Clock clock, AccountStore accounts) {
        this.clock = clock;
        this.accounts = accounts;
    }

    /** Creates (or resets) an ACTIVE account. Used for the first administrator from .env, never for sign-up. */
    public synchronized void addAccount(String username, String password, Role role) {
        String user = normalize(username);
        checkPassword(password);
        accounts.save(newRecord(user, password, role, AccountStatus.ACTIVE));
    }

    public synchronized boolean hasAccounts() { return !accounts.all().isEmpty(); }

    /**
     * Public sign-up. Students are active at once; staff and admin wait for an administrator.
     * @throws IllegalStateException when the username is taken
     */
    public synchronized AccountView signUp(String username, String password, Role role) {
        if (role == null) throw new IllegalArgumentException("Choose an account type: student, staff or admin");
        String user = normalize(username);
        if (!USERNAME.matcher(user).matches()) {
            throw new IllegalArgumentException("Username needs 3-32 letters, digits, dots, dashes or underscores");
        }
        checkPassword(password);
        Instant now = clock.instant();
        while (!signUps.isEmpty() && signUps.peekFirst().isBefore(now.minus(LOCK_WINDOW))) signUps.pollFirst();
        if (signUps.size() >= MAX_SIGNUPS_PER_MINUTE) throw new RateLimitedException("Too many sign-ups right now. Try again in a minute.");
        if (accounts.find(user).isPresent()) throw new IllegalStateException("This username is already taken");
        if (role != Role.STUDENT && pendingCount() >= MAX_PENDING) {
            throw new RateLimitedException("Too many requests are waiting for approval. Ask the administrator.");
        }
        signUps.addLast(now);
        AccountStatus status = role == Role.STUDENT ? AccountStatus.ACTIVE : AccountStatus.PENDING;
        AccountRecord record = newRecord(user, password, role, status);
        accounts.save(record);
        return AccountView.of(record);
    }

    /** All accounts for the administrator: waiting requests first, then the others by creation time. */
    public synchronized List<AccountView> listAccounts() {
        return accounts.all().stream().map(AccountView::of)
                .sorted(Comparator.comparing((AccountView a) -> a.status() != AccountStatus.PENDING)
                        .thenComparing(AccountView::createdAt).thenComparing(AccountView::username))
                .toList();
    }

    /** Lets a PENDING or REJECTED account sign in. */
    public synchronized AccountView approve(String actor, String username) {
        AccountRecord a = target(actor, username);
        AccountRecord updated = a.withStatus(AccountStatus.ACTIVE);
        accounts.save(updated);
        return AccountView.of(updated);
    }

    /** Declines a request or switches an account off. Its open sessions end at once. */
    public synchronized AccountView reject(String actor, String username) {
        AccountRecord a = target(actor, username);
        AccountRecord updated = a.withStatus(AccountStatus.REJECTED);
        accounts.save(updated);
        sessions.values().removeIf(s -> s.username().equals(a.username()));
        return AccountView.of(updated);
    }

    private AccountRecord target(String actor, String username) {
        String user = normalize(username);
        if (user.equals(normalize(actor))) throw new IllegalArgumentException("You cannot change your own account");
        return accounts.find(user).orElseThrow(() -> new NotFoundException("No account named " + user));
    }

    private long pendingCount() {
        return accounts.all().stream().filter(a -> a.status() == AccountStatus.PENDING).count();
    }

    private AccountRecord newRecord(String user, String password, Role role, AccountStatus status) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return new AccountRecord(user, salt, hash(password, salt), role, status, clock.instant());
    }

    private static void checkPassword(String password) {
        if (password == null || password.length() < 8) throw new IllegalArgumentException("Password must have at least 8 characters");
        if (password.length() > 128) throw new IllegalArgumentException("Password is too long (128 characters at most)");
    }

    private static String normalize(String username) { return username == null ? "" : username.trim().toLowerCase(); }

    public synchronized Login login(String username, String password) {
        String user = normalize(username);
        Instant now = clock.instant();
        Deque<Instant> recent = recentFailures(user, now);

        AccountRecord a = accounts.find(user).orElse(null);
        // Always hash, even for unknown users, so response time does not reveal which usernames exist.
        byte[] salt = a == null ? new byte[16] : a.salt();
        byte[] candidate = hash(password == null ? "" : password, salt);
        boolean ok = a != null && MessageDigest.isEqual(candidate, a.hash());
        if (!ok) {
            recent.addLast(now);
            throw new AuthenticationException("Invalid username or password");
        }
        recent.clear();
        // Only after the password was right do we say why an account cannot sign in yet.
        if (a.status() == AccountStatus.PENDING) throw new AuthenticationException("Your account is waiting for approval by the administrator");
        if (a.status() == AccountStatus.REJECTED) throw new AuthenticationException("Your account request was not approved");
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        Instant expires = now.plus(TOKEN_LIFETIME);
        sessions.put(token, new Session(user, a.role(), expires));
        return new Login(token, user, a.role(), expires);
    }

    /** Wrong-password times of one username inside the lock window. Throws when the username is locked. */
    private Deque<Instant> recentFailures(String user, Instant now) {
        Deque<Instant> recent = failures.computeIfAbsent(user, k -> new ArrayDeque<>());
        while (!recent.isEmpty() && recent.peekFirst().isBefore(now.minus(LOCK_WINDOW))) recent.pollFirst();
        if (recent.size() >= MAX_FAILURES) throw new RateLimitedException("Too many failed attempts. Try again in a minute.");
        return recent;
    }

    /** @return the session's login data, or empty when the token is unknown or expired */
    public synchronized Optional<Login> authenticate(String token) {
        if (token == null) return Optional.empty();
        Session s = sessions.get(token);
        if (s == null) return Optional.empty();
        if (!s.expiresAt().isAfter(clock.instant())) { sessions.remove(token); return Optional.empty(); }
        return Optional.of(new Login(token, s.username(), s.role(), s.expiresAt()));
    }

    /**
     * A signed-in user changes their own password. The current password must be right; a wrong one counts toward
     * the same lockout as a wrong login. Every other session of this user ends, the one that made the change stays.
     * @throws AccessDeniedException when the current password is wrong
     */
    public synchronized void changePassword(String username, String keepToken, String oldPassword, String newPassword) {
        String user = normalize(username);
        checkPassword(newPassword);
        Instant now = clock.instant();
        Deque<Instant> recent = recentFailures(user, now);
        AccountRecord a = accounts.find(user).orElseThrow(() -> new NotFoundException("No account named " + user));
        byte[] candidate = hash(oldPassword == null ? "" : oldPassword, a.salt());
        if (!MessageDigest.isEqual(candidate, a.hash())) {
            recent.addLast(now);
            throw new AccessDeniedException("Current password is wrong");
        }
        if (newPassword.equals(oldPassword)) throw new IllegalArgumentException("The new password must be different from the current one");
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        accounts.save(new AccountRecord(a.username(), salt, hash(newPassword, salt), a.role(), a.status(), a.createdAt()));
        recent.clear();
        sessions.entrySet().removeIf(e -> e.getValue().username().equals(user) && !e.getKey().equals(keepToken));
    }

    /** A random password for {@link #resetPassword}, made with the same SecureRandom as the salts. */
    String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (int i = 0; i < TEMPORARY_PASSWORD_LENGTH; i++) sb.append(TEMPORARY_ALPHABET.charAt(random.nextInt(TEMPORARY_ALPHABET.length())));
        return sb.toString();
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
