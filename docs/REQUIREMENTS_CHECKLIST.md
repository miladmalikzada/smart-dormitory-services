# Final defence checklist (week 15, 10 minutes)

| Requirement from the committee | Where it is | How to show it |
|---|---|---|
| Working website | `src/main/resources/web/` (HTML, CSS, JavaScript) | `bash scripts/run-web.sh`, open http://localhost:8080 |
| Frontend code | `web/index.html`, `web/style.css`, `web/app.js` | Open `app.js`: `api()` calls the backend with fetch |
| Backend code | `src/main/java/com/dorm/` (domain, service, infra, web) | Show the layers and `ArchitectureTest` |
| Working API endpoints | `web/Api.java`, table in `docs/API.md` | Website actions, or curl commands from `docs/API.md` |
| Connected database | SQLite: `schema.sql`, `infra/SqliteRepositories.java` | `python3 scripts/show-db.py` while the site is running |
| CRUD operations | Students, rooms, tickets, visitors, announcements | Create, edit, delete in the website; `ApiTest` proves each one |
| Live demo | `docs/DB_DEMO.md`, `docs/DEFENCE_PREP.md` | 10-minute order below |
| GitHub: commits, branches, teamwork | GitHub repository | Show Pull requests, Commits, branches, CI badge, release tags |

## 10-minute plan
1. (1 min) Problem and architecture slide.
2. (4 min) Website live demo: add student, request room, undo, report problem, switch to STAFF and resolve it, post announcement, visitor arrives and leaves. Point out the activity feed on the dashboard.
3. (2 min) Database: `python3 scripts/show-db.py` shows the same data in SQLite tables, and the database refusing invalid data.
4. (1 min) API: one curl command and the endpoint table.
5. (2 min) GitHub: pull requests with reviews, each member's commits, green CI, tags.
