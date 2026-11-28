# ADR-0015: An administrator can reset a forgotten password

Status: accepted · Date: after v1.3.1

**Context:** Users can change their own password (US18), but nothing helped someone who forgot it. LIMITATIONS item 3 admitted that such an account was lost.

**Options:** (A) an e-mail link; (B) the administrator sets a password chosen by the administrator; (C) the administrator triggers a random temporary password that the server creates and shows once.

**Decision:** C. There is no mail server in this project, so A is not possible. B lets the administrator know a password that the user will keep. In C the server makes 12 random characters (no look-alike characters) with the same `SecureRandom` as the salts, stores only the salted PBKDF2 hash, returns the clear text once in the answer to the administrator and never again. The reset ends the user's sessions and clears their lockout. An administrator cannot reset their own account (the same rule as approve and reject); the owner `admin` resets its password in `.env`.

**Consequences:** Anyone with an administrator account can lock a user out by resetting; this is accepted because administrators can already switch accounts off. The user is not forced to change the temporary password (LIMITATIONS item 3). Tests: `AuthServiceTest`, `AuthApiTest`, `SqliteRepositoryTest`.
