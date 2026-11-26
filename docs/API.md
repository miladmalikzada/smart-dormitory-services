# REST API

Base URL: `http://localhost:8080/api`. Data format: JSON. Send the header `X-Role: STUDENT | STAFF | ADMIN`
(default STUDENT). Errors look like `{"error":"message"}`.

| Resource | GET (read) | POST (create) | PUT (update) | DELETE |
|---|---|---|---|---|
| `/students` | list / `/{id}` | `{name, phone}` | `/{id}` `{name, phone}` | `/{id}` (refused while housed or hosting visitors) |
| `/rooms` | list / `/{id}` | `{id, capacity}` | `/{id}` `{capacity}` | `/{id}` (only empty rooms without tickets); `/{id}/occupants/{studentId}` checks a student out |
| `/room-requests` | - | `{studentId}` assigns a room (Strategy + Command); `/undo` undoes the last room action (Memento) | - | - |
| `/tickets` | list / `/{id}` | `{roomId, description, priority}` | `/{id}` `{action: assign/start/resolve, technician}` (State; STAFF/ADMIN only via Proxy) | `/{id}` (STAFF/ADMIN) |
| `/visitors` | list / `/inside` / `/{id}` | `{visitorName, hostStudentId}` | `/{id}` records departure | `/{id}` |
| `/announcements` | list / `/{id}` | `{title, body}` | `/{id}` `{title, body}` | `/{id}` |
| `/summary` | counts for the dashboard | - | - | - |
| `/events` | last 30 events (Observer audit log) | - | - | - |

Status codes: `200` ok, `201` created, `400` invalid input, `403` role not allowed, `404` unknown id,
`405` method not allowed, `409` rule violated (e.g. illegal ticket step, room not empty).

## Try it with curl
```bash
curl -s localhost:8080/api/students -H 'Content-Type: application/json' -d '{"name":"Ayan","phone":"777"}'
curl -s localhost:8080/api/students
curl -s -X POST localhost:8080/api/room-requests -H 'Content-Type: application/json' -d '{"studentId":"S-xxxxxx"}'
curl -s -X POST localhost:8080/api/tickets -H 'Content-Type: application/json' -d '{"roomId":"R101","description":"Heater","priority":"HIGH"}'
curl -s -X PUT localhost:8080/api/tickets/T-xxxxxx -H 'Content-Type: application/json' -H 'X-Role: STAFF' -d '{"action":"assign","technician":"Aibek"}'
curl -s -X DELETE localhost:8080/api/announcements/A-xxxxxx
```

## Where the code is
* `web/Api.java` - all endpoints and error mapping (only calls services, unit-tested in `ApiTest`)
* `web/WebServer.java` - HTTP adapter on the JDK's built-in server, also serves the website
* `web/Json.java` - small JSON reader/writer
* `src/main/resources/web/` - the website: `index.html`, `style.css`, `app.js`
