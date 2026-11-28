# Final defence checklist (week 15, 10 minutes)

| Requirement from the committee | Where it is | How to show it |
|---|---|---|
| Working website | `src/main/resources/web/` (HTML, CSS, JavaScript) | `bash scripts/run-web.sh`, open http://localhost:8080 |
| Frontend code | `web/index.html`, `web/style.css`, `web/app.js` | Open `app.js`: `api()` calls the backend with fetch |
| Backend code | `src/main/java/com/dorm/` (domain, service, infra, web) | Show the layers and `ArchitectureTest` |
| Working API endpoints | `web/Api.java`, table in `docs/API.md` | Website actions, or curl commands from `docs/API.md` |
| Connected database | SQLite: `schema.sql`, `infra/SqliteRepositories.java` | `python3 scripts/show-db.py` while the site is running |
| CRUD operations | Students, rooms, tickets, visitors, announcements | Create, edit, delete in the website; `ApiTest` proves each one |
| Security (`.env`, login, encryption) | `.env.example`, `scripts/setup_env.py`, `AuthService`, `AesGcmCipher`, `docs/SECURITY.md` | Sign in as admin / staff / student: the same student shows full, masked or hidden identity numbers; `show-db.py` shows the encrypted columns |
| Accounts: sign-up, approval, change password | `service/AuthService`, `infra/SqliteAccountStore`, ADR-0013, UML 13 and 15 | Sign up as a student (works at once), sign up as staff (waits), approve it as admin on the Accounts page, change a password from the sidebar |
| Room administration and automatic room | `service/SecuredRoomAdminService`, `RoomAdminService`, ADR-0014, UML 14 | As admin add a room with its beds; add a student with "give a room automatically" ticked; as staff try to add a room and get 403 |
| Student details (IIN, date of birth, passport, national ID...) | `domain/Student`, `domain/Iin`, `StudentService` | Add a student in the website; try a wrong IIN and see the error |
| Documents and export | `service/CertificateService`, `CertificateFormat` (Strategy), `StudentExporter` | Students page: **Certificate** button per housed student, **Export CSV** in the toolbar |
| Live demo | `docs/DB_DEMO.md`, `docs/DEFENCE_PREP.md` | 10-minute order below |
| GitHub: commits, branches, teamwork | GitHub repository | Show Pull requests, Commits, branches, CI badge, release tags |

## 10-minute plan
1. (1 min) Problem and architecture slide.
2. (4 min) Website live demo: sign in as admin, add a student with IIN and passport, request room, undo, report problem, sign in as staff (masked IIN) and resolve it, post announcement, visitor arrives and leaves. Point out the activity feed on the dashboard.
3. (2 min) Database: `python3 scripts/show-db.py` shows the same data in SQLite tables, and the database refusing invalid data.
4. (1 min) API: one curl command and the endpoint table.
5. (2 min) GitHub: pull requests with reviews, each member's commits, green CI, tags.
