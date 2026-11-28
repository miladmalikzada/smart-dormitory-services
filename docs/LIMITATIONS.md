# Known limitations and future work

An honest list of what this version does **not** do. Each item says why it was left out and what the fix would be.

| # | Limitation | Why it is acceptable now | How to fix it |
|---|---|---|---|
| 1 | **Sessions are kept in memory.** A server restart signs everyone out. The login lockout counters are also reset. | One server process, 8-hour tokens, demo scale. | Store sessions in the database (new `sessions` table behind `AuthService`), or use signed tokens. |
| 2 | **HTTP only, no TLS.** | The app is meant to run behind a reverse proxy (nginx, Caddy) or on localhost. | Terminate TLS at the proxy and set the `Secure` flag on any cookie. |
| 3 | **No password reset, and no e-mail check at sign-up.** Sign-up, approval (ADR-0013) and changing your own password exist, but a forgotten password cannot be recovered. The owner password comes from `.env` at every start. | Keeps the first version small. | A reset that the administrator triggers or an e-mail link. |
| 4 | **No audit log.** Reads and changes of identity data are not recorded. | The events list only covers business events. | Add an `AuditLog` observer on `SecuredStudentService` that writes who/what/when. |
| 5 | **Encryption key is one key from `.env`.** There is no rotation. | Per-value random IV already protects against pattern leaks. | Key id stored beside each ciphertext, so old and new keys can both decrypt during rotation. |
| 6 | **IIN and national ID cannot be searched by partial value** (they are encrypted). | Searching encrypted fields would weaken the protection. | Store a keyed hash (HMAC) next to each value for exact-match lookup. |
| 7 | **No load or concurrency tests.** The server uses the JDK `HttpServer` with a small thread pool. | Dormitory-sized load. | Add a concurrency test for room allocation and a simple load test. |
| 8 | **The website is tested by hand and by a local browser script, not in CI.** | CI runs unit, API, database and architecture tests. | Add a Playwright job to the workflow. |
| 9 | **`web/Api.java` is the largest class (about 380 lines).** It routes every resource. | It is still one readable file, grouped by resource. | Split into one small router class per resource (`StudentRoutes`, `TicketRoutes`, ...). |
| 10 | **No pagination.** Lists return all records. | A dormitory has hundreds of students, not millions. | Add `limit` and `offset` query parameters. |

## Process limitations

- The project history is short. The code was developed and integrated in a few days, not over the 15-week plan in [TEAM_WORKFLOW.md](TEAM_WORKFLOW.md).
- Teammates' work is tracked as GitHub issues and pull requests. See [FRIEND_GUIDE.md](FRIEND_GUIDE.md) for how each task is done and reviewed.
