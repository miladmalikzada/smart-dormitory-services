# Database design

The system stores data behind the `Repository` interface. Three implementations exist:
`InMemoryRepository` (tests, demo), `FileRepository` (Java serialisation) and the **SQLite** repositories
(relational database through JDBC). Start with `--storage=sqlite`; the file is `data/dorm.db`.

## ER diagram
```mermaid
erDiagram
  STUDENTS ||--o{ ROOM_OCCUPANTS : "lives in"
  ROOMS ||--o{ ROOM_OCCUPANTS : "houses"
  ROOMS ||--o{ TICKETS : "has problems"
  STUDENTS ||--o{ VISITORS : "hosts"
  STUDENTS {
    text id PK
    text first_name
    text last_name
    text date_of_birth
    text gender "MALE FEMALE"
    text nationality
    text iin "ENCRYPTED"
    text national_id_number "ENCRYPTED"
    text passport_number "ENCRYPTED"
    text phone
    text email
    text faculty
    int course_year "1-8"
    text address
    text emergency_contact_name
    text emergency_contact_phone
  }
  ROOMS {
    text id PK
    int capacity "CHECK > 0"
  }
  ROOM_OCCUPANTS {
    text room_id PK,FK
    text student_id PK,FK "UNIQUE: one room per student"
    int position
  }
  TICKETS {
    text id PK
    text room_id FK
    text description
    text priority "LOW MEDIUM HIGH URGENT"
    text status "OPEN ASSIGNED IN_PROGRESS RESOLVED"
    text technician
    text created_at
  }
  VISITORS {
    text id PK
    text visitor_name
    text host_student_id FK
    text arrived_at
    text departed_at "NULL = still inside"
  }
  ANNOUNCEMENTS {
    text id PK
    text title
    text body
    text posted_at
  }
```

## Design decisions
* **Normalisation (3NF):** occupants are not a list inside `rooms`; they are rows of `room_occupants`. A student appears in one place only.
* **Business rules in the schema:** `UNIQUE(student_id)` means a student cannot have two rooms even if the code had a bug; `CHECK` constraints limit priority and status values; foreign keys block tickets for unknown rooms and visitors for unknown hosts (`PRAGMA foreign_keys = ON`).
* **NULL means something:** `departed_at IS NULL` is a visitor who is still inside.
* **Dates** are stored as ISO-8601 text, which sorts correctly and needs no driver-specific types.
* **Why no database in the domain:** the domain and service layers do not import `java.sql`. Only `infra` does (checked by `ArchitectureTest`).
* **Why SQLite:** no server to install, one file, full SQL and transactions, so tests run offline. A server database (PostgreSQL) would only need new classes behind the same interface.

## Example queries the schema supports
```sql
-- free beds per room
SELECT r.id, r.capacity - COUNT(o.student_id) AS free_beds
FROM rooms r LEFT JOIN room_occupants o ON o.room_id = r.id GROUP BY r.id;

-- open urgent tickets
SELECT id, room_id, description FROM tickets WHERE priority = 'URGENT' AND status <> 'RESOLVED';

-- visitors still inside
SELECT visitor_name, host_student_id, arrived_at FROM visitors WHERE departed_at IS NULL;
```

## Personal data and encryption
Student records hold personal data, so three columns are protected:

| Column | Protection |
|---|---|
| `iin`, `national_id_number`, `passport_number` | Encrypted by the application with **AES-256-GCM** before they are written. A stolen `dorm.db` shows only random-looking text. |
| other columns | Stored as normal text (names, faculty, phone...) so SQL reports still work. |

* The key is `DORM_ENCRYPTION_KEY` in the private `.env` file (never in Git). Every value has its own random IV.
* Because the same number encrypts to different text each time, SQL `UNIQUE` cannot be used on these columns;
  `StudentService` checks uniqueness instead (tested in `ApiTest.duplicateIdentityNumbersAreRejected`).
* Run `python3 scripts/show-db.py` to show the professor the encrypted columns next to the readable ones.
* The `accounts` table (username, salt, password_hash, role, status, created_at) holds the login accounts. It has no link to `students`, because a student account is a login, not a record. Passwords are stored only as PBKDF2 hashes.
* An older `data/dorm.db` (before this change) has a different `students` table: delete the `data/` folder once.
