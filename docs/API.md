# REST API

Base URL: `http://localhost:8080/api`. Data format: JSON. Every request except `POST /api/login` and `POST /api/signup` needs the header
`Authorization: Bearer <token>`. Errors look like `{"error":"message"}`.

| Endpoint | Meaning |
|---|---|
| `POST /api/login` `{username, password}` | returns `{token, username, role, expiresAt}` (token valid 8 hours). Wrong password: **401**; correct password but account still waiting or declined: **401** with the reason; 5 wrong attempts in a minute: **429** |
| `POST /api/signup` `{username, password, role}` | public. `role` is `STUDENT`, `STAFF` or `ADMIN`. Returns **201** with `{username, role, status, message}`; `status` is `ACTIVE` for students and `PENDING` for staff and admin. Bad input: **400**; username taken: **409**; too many sign-ups: **429** |
| `GET /api/accounts` | administrator only (others **403**): all accounts, waiting requests first. Never contains passwords or hashes |
| `POST /api/accounts/{username}/approve` | administrator only: lets a waiting or switched-off account sign in |
| `POST /api/accounts/{username}/reject` | administrator only: declines a request or switches an account off and ends its sessions. You cannot change your own account (**400**) |
| `GET /api/me` | who am I (needs token) |
| `POST /api/accounts/{user}/reset-password` | administrator only (others **403**). Gives that account a new random 12-character temporary password and returns `{username, temporaryPassword}` once. The old password stops working, the account's open sessions end and its lockout is cleared. Own account: **400** (use change password); unknown account: **404** |
| `POST /api/me/password` `{oldPassword, newPassword}` | changes your own password. Returns **200** `{changed:true}`. Wrong current password: **403** (counts toward the 5-attempt lockout, **429**); new password under 8 characters or equal to the old one: **400**. Your other sessions end, this one stays |
| `POST /api/logout` | invalidates the token |

Accounts: the owner account `admin` (password from `.env`), and everyone else who signed up and, for staff and
admin, was approved (see [SECURITY.md](SECURITY.md)). Status codes: 400 bad input, 401 not signed in, 403 role not allowed,
404 not found, 405 method, 409 conflict, 413 request over 64 KB, 429 too many logins.

**Student data by role:** ADMIN sees and changes everything. STAFF sees students read-only with IIN, national ID
and passport **masked** (`********1237`). STUDENT sees only a directory entry (id, name, faculty, course year).

| Resource | GET (read) | POST (create) | PUT (update) | DELETE |
|---|---|---|---|---|
| `/students` | list / `/{id}` | student fields below (ADMIN only). Add `"assignRoom": true` to also place the new student in the room with the most free beds: the answer then has `roomId`, or `roomMessage` when no bed is free (the student is still saved) | `/{id}` same fields (ADMIN only; identity numbers left out keep their value) | `/{id}` (ADMIN only; refused while housed or hosting visitors) |
| `/rooms` | list / `/{id}` (everybody) | `{id, capacity}` (ADMIN only) | `/{id}` `{capacity}` (ADMIN only) | `/{id}` (ADMIN only; only empty rooms without tickets); `/{id}/occupants/{studentId}` checks a student out |
| `/room-requests` | - | `{studentId}` assigns a room (Strategy + Command); `/undo` undoes the last room action (Memento) | - | - |
| `/tickets` | list / `/{id}` | `{roomId, description, priority}` | `/{id}` `{action: assign/start/resolve, technician}` (State; STAFF/ADMIN only via Proxy) | `/{id}` (STAFF/ADMIN) |
| `/visitors` | list / `/inside` / `/{id}` | `{visitorName, hostStudentId}` | `/{id}` records departure | `/{id}` |
| `/announcements` | list / `/{id}` | `{title, body}` | `/{id}` `{title, body}` | `/{id}` |
| `/certificates` | `/{studentId}/{text\|html\|csv}` returns `{filename, contentType, content}`; STAFF/ADMIN; 409 if the student has no room | - | - | - |
| `/export/students` | CSV of students as the caller's role may see them (staff: masked); STAFF/ADMIN | - | - | - |
| `/summary` | counts for the dashboard | - | - | - |
| `/events` | last 30 events (Observer audit log) | - | - | - |

Success codes: `200` ok, `201` created. Error codes are listed at the top.

## Try it with curl
```bash
TOKEN=$(curl -s localhost:8080/api/login -d '{"username":"admin","password":"<from .env>"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
curl -s localhost:8080/api/students -H "Authorization: Bearer $TOKEN" -d '{"firstName":"Ayan","lastName":"Nurlan","dateOfBirth":"2004-05-17","iin":"040517501237","faculty":"Information Systems","courseYear":2}'
curl -s localhost:8080/api/students -H "Authorization: Bearer $TOKEN"
curl -s -X POST localhost:8080/api/room-requests -H "Authorization: Bearer $TOKEN" -d '{"studentId":"S-xxxxxx"}'
curl -s -X POST localhost:8080/api/tickets -H "Authorization: Bearer $TOKEN" -d '{"roomId":"R101","description":"Heater","priority":"HIGH"}'
curl -s -X PUT localhost:8080/api/tickets/T-xxxxxx -H "Authorization: Bearer $TOKEN" -d '{"action":"assign","technician":"Aibek"}'
curl -s -X DELETE localhost:8080/api/announcements/A-xxxxxx -H "Authorization: Bearer $TOKEN"
```

## Where the code is
* `web/Api.java` - all endpoints and error mapping (only calls services, unit-tested in `ApiTest`)
* `web/WebServer.java` - HTTP adapter on the JDK's built-in server, also serves the website
* `web/Json.java` - small JSON reader/writer
* `src/main/resources/web/` - the website: `index.html`, `style.css`, `app.js`

## Student fields
| Field | Rules |
|---|---|
| `firstName`, `lastName` | required |
| `dateOfBirth` | required, `YYYY-MM-DD`, not in the future, must match the IIN |
| `iin` | required, 12 digits, valid check digit, unique. Encrypted in the database |
| `nationalId`, `passportNumber` | optional, 5-20 letters/digits, unique. Encrypted in the database |
| `gender` | `MALE` or `FEMALE` |
| `nationality`, `phone`, `email`, `faculty`, `address`, `emergencyName`, `emergencyPhone` | optional (`email` must look like an email) |
| `courseYear` | optional, 1-8 |
