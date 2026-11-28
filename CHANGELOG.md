# Changelog

## 1.4.0 - passwords, floor strategy, tests and documents
* **Reset password:** an administrator can give an account a random temporary password (`POST /api/accounts/{user}/reset-password`, button on the Accounts page). The old password stops working, the user's sessions end and their lockout is cleared. ADR-0015.
* **Change password:** signed-in users change their own password (`POST /api/me/password`, form on the website). The current password is required, wrong tries count toward the login lockout, and other sessions end.
* **Floor strategy:** new `FloorStrategy` fills one floor before the next (R101 is floor 1). It is not the default; change `new LeastOccupiedStrategy()` to `new FloorStrategy()` in `App.wire` to use it.
* **Tests:** iterator, API validation, request size, room administration proxy, account store, sign-up limits and 12 students asking for 7 beds over real HTTP.
* **Documents:** acceptance criteria for US1-US19, UML diagrams 13-15, ADR-0014 and ADR-0015, pattern table in the README.
* **Fixes in the documents:** the database has 7 tables (README and DB demo said 6).

## 1.3.1 - rooms and automatic room for new students
- Fix: the *Add room* form on the Rooms page did nothing (an input called `id` hid the form's own id). Admins can add rooms and set their number of beds again.
- Security: adding, resizing and deleting rooms is now ADMIN only (new `SecuredRoomAdminService`); before, any role could call the API. Staff and students see rooms read-only.
- New students can be given a room automatically (tick box in the Add student form, `assignRoom` in the API); a full dormitory gives a clear message and the student is still saved.

## 1.3.0 - professional design
- New design system: dark gradient sidebar, card layout, colour-coded dashboard statistics, refined tables, buttons, badges and forms.
- Sign-in page with an illustrated campus scene; welcome banner on the dashboard; a small picture and subtitle on every page; friendly empty states.
- Original SVG illustrations in `src/main/resources/web/img/` (no third-party images, so no licence issues).
- Mobile layout fixed: scrolling menu, no sideways overflow. The server can now also serve PNG, JPEG and WebP pictures.

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
