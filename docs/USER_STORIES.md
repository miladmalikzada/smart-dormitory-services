# User stories (M1)

| # | Story | Acceptance criteria |
|---|---|---|
| US1 | As an administrator I register a student with full details | First/last name, date of birth, valid IIN (matches birth date) required; passport, national ID, contacts optional; duplicates rejected |
| US2 | As a student I request a room | Gets a room with a free bed; error if none; cannot hold two rooms |
| US3 | As a student I undo my last room action | Room occupants are exactly as before |
| US4 | As a student I report a problem in a room | Room must exist; ticket starts OPEN with a priority |
| US5 | As staff I assign, start and resolve tickets | Illegal order is rejected (OPEN cannot start) |
| US6 | As a student I must not change tickets | Student role gets "Access denied" |
| US7 | As a guard I record a visitor arriving for a registered student | Unknown host is rejected |
| US8 | As a guard I record departure and list visitors still inside | Departed visitors are not listed |
| US9 | As an administrator I post an announcement | All channels (console, SMS) deliver it |
| US10 | As an administrator I keep data between runs | `--storage=file` restores rooms, tickets, announcements |
| US11 | As staff I look up a student without seeing full identity numbers | IIN, passport and national ID shown as `********1237` |
| US12 | As a student I see only the student directory | No IIN, phone, e-mail or address in any response |
| US13 | As any user I must sign in | No token: 401; wrong password: 401; five wrong tries: 429 |
| US14 | As a new student I create my own account | Sign-up as student: account is active, I can sign in at once, my password is not stored in clear text |
| US15 | As the administrator I decide who becomes staff or admin | Staff/admin sign-ups stay PENDING and cannot sign in until I approve them; a declined account is refused; nobody can approve themselves |
| US16 | As the administrator I add rooms and set how many beds each has | Only ADMIN can add, resize or delete a room; staff and students get 403 but can look at rooms |
| US17 | As the administrator I want a new student to get a room straight away | With "give a room automatically" ticked, the student is placed in the room with most free beds; if the dormitory is full the student is still saved and I see a clear message |

## Acceptance criteria

Each criterion names who acts, what they do and the result you can check (a status code, a value in a list or a stored value). The tests named in `docs/PATTERN_REGISTER.md` and `docs/REQUIREMENTS_CHECKLIST.md` cover them.

### US1 - Register a student
* When an administrator sends first name, last name, date of birth 2004-05-17 and IIN 040517501237, the answer is 201 and the student appears in the student list.
* When the IIN has a wrong check digit, or its date does not match the date of birth, the answer is 400 and nothing is saved.
* When a second student is sent with an IIN that is already stored, the answer is 409.

### US2 - Request a room
* When a room has a free bed and a student requests a room, the student id appears in that room's occupants.
* When every bed in every room is taken, the request fails with an error and no room changes.
* When a student who already has a room asks again, the request fails and the student keeps the first room.

### US3 - Undo the last room action
* After a room request, undoing it leaves the room's occupants exactly as they were before the request.
* When there is nothing left to undo, the answer says so and nothing changes.

### US4 - Report a problem in a room
* When a student reports a problem for an existing room with priority "high", a ticket is created with status OPEN and the priority "high".
* When the room does not exist, the answer is 404 and no ticket is created.
* When the priority is not one of the allowed values, the answer is 400.

### US5 - Assign, start and resolve tickets
* When staff assign an OPEN ticket the status becomes ASSIGNED; start makes it IN_PROGRESS; resolve makes it RESOLVED.
* When staff try to start a ticket that is still OPEN, the answer is 409 and the status stays OPEN.

### US6 - Students must not change tickets
* When a student tries to assign, start, resolve or delete a ticket, the answer is 403 and the ticket is unchanged.
* A student can still create a ticket (US4) and read tickets.

### US7 - Record a visitor arriving
* When the host student exists, the arrival is recorded and the visitor is listed as inside.
* When the host id does not exist, the answer is 404 and no visit is recorded.

### US8 - Record a departure and list visitors still inside
* After a departure is recorded, that visitor no longer appears in the "inside" list.
* Recording the departure of the same visit twice gives 409.
* The "inside" list keeps visitors in the order they arrived.

### US9 - Post an announcement
* A posted announcement appears in the announcement list with its title and text.
* When the announcement is posted, the message is delivered on both channels (console and SMS).
* After the administrator edits the text, the list shows the new text.

### US10 - Keep data between runs
* With file or SQLite storage, rooms, tickets and announcements saved before a restart are present after it, with the same ids and text.
* With SQLite storage, a ticket that points to a room that does not exist is refused by the database itself.

### US11 - Staff see masked identity numbers
* When staff read a student, the IIN, passport and national ID show only the last four characters, for example ********1237.
* When the administrator reads the same student, the full values are shown.

### US12 - Students see only the directory
* When a student reads the student directory, no response contains an IIN, passport number, national ID, phone, e-mail or address.
* The directory still shows each student's name so residents can be found.

### US13 - Everyone must sign in
* A request without a valid token gets 401; so does a forged token or a non-Bearer header.
* A wrong password gets 401 with a generic message that does not say whether the username exists.
* After five wrong passwords for one username in a minute, the next attempt gets 429, even with the correct password.

### US14 - Students create their own account
* Signing up with role STUDENT and a password of at least 8 characters gives 201, status ACTIVE, and the student can sign in at once.
* A username shorter than 3 characters, a short password, an unknown role or a name that is already taken is refused (400 or 409) and no account is created.
* The database stores only a salt and a password hash; the password itself is never stored.
