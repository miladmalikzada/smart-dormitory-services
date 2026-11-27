# Changelog

## 1.2.0 - sign-up and account approval
- Sign-up page: username, password and account type (student, staff, administrator). Accounts and password hashes are stored in the SQLite `accounts` table.
- Students are active at once; staff and administrator requests wait for approval by the owner administrator (new *Accounts* page, API `/api/signup` and `/api/accounts`).
- Only the `admin` account from `.env` exists at the start. `DORM_STAFF_PASSWORD` and `DORM_STUDENT_PASSWORD` are no longer used.

## 1.1.0 - security, student records, certificates
- Login with expiring Bearer tokens; accounts and the encryption key come from the private `.env` file.
- Student records: IIN (validated), date of birth, national ID, passport, contacts, study and emergency data.
- IIN, national ID and passport are encrypted (AES-256-GCM) in SQLite; staff see them masked, students see a directory.
- New website design with sign-in page, sidebar, search, filters and forms.
- Accommodation certificates in text, HTML and CSV (Strategy pattern); CSV export of students.
- VS Code run configurations, CONTRIBUTING guide, pull request and issue templates.

## 1.0.0 - first complete version
- Room requests with undo, maintenance tickets (State), visitor log, announcements (Observer).
- Console menu, REST API, website, SQLite database, CI, 12 design patterns, architecture tests.
