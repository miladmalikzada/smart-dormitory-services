# ADR-0013: Sign-up with administrator approval, accounts in the database

Status: accepted · Date: after v1.0.0

**Context:** Version 1 had three fixed accounts from `.env`. The owner wants people to create their own accounts, remembered by the database, with the type chosen at sign-up: anyone may be a student, but staff and administrator rights need the owner's permission.

**Options:** (A) keep fixed accounts; (B) open sign-up where anyone picks any role; (C) sign-up where the role is a *request*: students are active, staff/admin are `PENDING` until an administrator approves; (D) invitation codes.

**Decision:** C. `AuthService` keeps `AccountRecord`s in an `AccountStore` interface (SQLite in production, memory for tests and the memory/file modes), the same dependency-inversion idea as `Repository` and `FieldCipher`, so the service layer stays free of infra. Passwords stay PBKDF2 hashes. The `admin` account from `.env` is written at every start, so the owner can never be locked out. Account management routes check the administrator role in `Api`.

**Consequences:** A role is never granted by the sign-up request alone. A stranger can create student accounts (they see only the directory); sign-ups are rate-limited and pending requests are capped. There is no e-mail check and no password reset yet (LIMITATIONS.md). With `--storage=memory` or `file`, accounts last only until the server stops; use `--storage=sqlite`.
