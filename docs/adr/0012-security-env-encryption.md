# ADR-0012: Secrets in .env, encrypted identity numbers, token login

Status: accepted · Date: week 14

**Context:** Students now have personal data (IIN, date of birth, national ID, passport). Passwords and keys must not be in the code or in Git, and a stolen database file must not reveal identity numbers. The first web version trusted an `X-Role` header, which anyone could change.

**Options:** (A) keep X-Role; (B) HTTP Basic with fixed passwords; (C) login endpoint + expiring Bearer tokens; for data: (D) plain text, (E) encrypt the whole file, (F) encrypt only the sensitive columns.

**Decision:** C and F. Secrets come from `.env` (`EnvConfig`, ignored by Git). `AuthService` verifies PBKDF2 hashes and issues tokens; `Api.handleRequest` is the only entry and maps the token to a role. `SecuredStudentService` is a Protection Proxy (same pattern as ADR-0004) that gives ADMIN full data, STAFF masked data and STUDENT a directory view. `AesGcmCipher` (behind the `FieldCipher` interface, so the service layer stays free of infra) encrypts three columns.

**Consequences:** Uniqueness of encrypted numbers is checked in `StudentService`, not by SQL. Losing the key loses those values. Old databases must be recreated (`SqliteDatabase` tells the user). Roles are no longer selectable in the browser; the tests call `Api.handle(..., role)` directly or log in.
