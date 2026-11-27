# Security

| Topic | What the project does | Where |
|---|---|---|
| Secrets | Passwords and the encryption key live in the private **`.env`** file. It is listed in `.gitignore`, so it is never committed. `.env.example` (committed) shows the names only. | `.env`, `.env.example`, `infra/EnvConfig` |
| Create `.env` | `python3 scripts/setup_env.py` writes random passwords and a random 256-bit key and sets file mode 600. `run-web.sh` does it automatically the first time. | `scripts/setup_env.py` |
| Encryption at rest | IIN, national ID number and passport number are encrypted with **AES-256-GCM** (random IV per value, tamper-detecting) before they reach SQLite. | `infra/AesGcmCipher`, `infra/SqliteRepositories.Students` |
| Passwords | Never stored in code. Hashed with **PBKDF2-HMAC-SHA256** (120,000 rounds, random salt), compared in constant time. | `service/AuthService` |
| Sessions | Random 256-bit Bearer token, expires after 8 hours, destroyed on logout. | `service/AuthService`, `web/Api.handleRequest` |
| Accounts and sign-up | Anyone can sign up as a **student** (active at once). **Staff** and **admin** requests are `PENDING` and cannot sign in until the administrator approves them on the Accounts page. Only the `admin` account from `.env` exists at the start; it is reset at every start, so the owner cannot be locked out. An administrator cannot change their own account. Switching an account off ends its open sessions at once. Wrong passwords never reveal whether an account is pending. Sign-ups are limited to 20 per minute and 50 waiting requests. | `service/AuthService`, `infra/SqliteAccountStore`, table `accounts` |
| Brute force | 5 wrong passwords in 60 seconds lock that user name for a minute (HTTP 429). Same message for unknown user and wrong password. | `service/AuthService` |
| Roles | ADMIN: everything. STAFF: students read-only with identity numbers masked (`********1237`); tickets. STUDENT: student directory only. Implemented as a **Protection Proxy**, so the real service has no role code. | `service/SecuredStudentService`, `SecuredTicketService` |
| Input validation | IIN check digit and birth-date match, document formats, email, course year, duplicates, 64 KB request limit. | `domain/Iin`, `service/StudentService`, `web/WebServer` |
| Browser | Content-Security-Policy (`script-src 'self'`), `X-Frame-Options: DENY`, `nosniff`, no referrer, no CORS (the page and API share one origin). All text is HTML-escaped in the page. | `web/WebServer`, `web/app.js` |
| SQL injection | Only `PreparedStatement` with `?` parameters. | `infra/SqlRepository`, `SqliteRepositories` |

## Rules for the team
1. Never commit `.env` (check with `git status` before every commit).
2. Do not share passwords in chat; each person runs `python3 scripts/setup_env.py` for their own copy.
3. **If the key in `.env` is lost or replaced, encrypted values in an existing `data/dorm.db` cannot be read.** Delete `data/` (demo data) or restore the old `.env`.
4. Use only synthetic IIN/passport numbers in demos and screenshots.

## Honest limits (good to say in the defence)
* No password reset or change yet, and no e-mail check at sign-up (see [LIMITATIONS.md](LIMITATIONS.md)).
* Plain HTTP on localhost. For a real deployment put it behind HTTPS (the token would otherwise be readable on the network).
* Date of birth, phone, e-mail and address are not encrypted (they stay searchable); only the three government identity numbers are.
* The key sits in an environment file, not a key vault. Production would use a secrets manager.
* File storage (`--storage=file`) is a demo option and is **not** encrypted; use `--storage=sqlite` for real data.
