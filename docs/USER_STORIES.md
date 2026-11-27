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
