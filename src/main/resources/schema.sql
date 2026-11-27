-- Smart Dormitory Services: SQLite schema (third normal form).
-- Loaded automatically by SqliteDatabase when the app starts with --storage=sqlite.
-- The database file itself (data/dorm.db) is created at run time and is not stored in Git.

-- iin, national_id_number and passport_number are stored ENCRYPTED (AES-256-GCM, key from .env),
-- so a stolen dorm.db file does not reveal them. Because every encryption uses a random IV,
-- uniqueness of these three values is checked by the application (StudentService), not by SQL.
CREATE TABLE IF NOT EXISTS students (
  id                      TEXT PRIMARY KEY,
  first_name              TEXT NOT NULL,
  last_name               TEXT NOT NULL,
  date_of_birth           TEXT,
  gender                  TEXT CHECK (gender IN ('MALE','FEMALE')),
  nationality             TEXT,
  iin                     TEXT,
  national_id_number      TEXT,
  passport_number         TEXT,
  phone                   TEXT,
  email                   TEXT,
  faculty                 TEXT,
  course_year             INTEGER CHECK (course_year BETWEEN 1 AND 8),
  address                 TEXT,
  emergency_contact_name  TEXT,
  emergency_contact_phone TEXT
);

CREATE TABLE IF NOT EXISTS rooms (
  id       TEXT PRIMARY KEY,
  capacity INTEGER NOT NULL CHECK (capacity > 0)
);

-- One row per resident. UNIQUE(student_id) = a student can live in only one room.
CREATE TABLE IF NOT EXISTS room_occupants (
  room_id    TEXT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
  student_id TEXT NOT NULL UNIQUE REFERENCES students(id),
  position   INTEGER NOT NULL,
  PRIMARY KEY (room_id, student_id)
);

CREATE TABLE IF NOT EXISTS tickets (
  id          TEXT PRIMARY KEY,
  room_id     TEXT NOT NULL REFERENCES rooms(id),
  description TEXT NOT NULL,
  priority    TEXT NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
  status      TEXT NOT NULL CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','RESOLVED')),
  technician  TEXT,
  created_at  TEXT NOT NULL
);

-- departed_at IS NULL means the visitor is still inside.
CREATE TABLE IF NOT EXISTS visitors (
  id              TEXT PRIMARY KEY,
  visitor_name    TEXT NOT NULL,
  host_student_id TEXT NOT NULL REFERENCES students(id),
  arrived_at      TEXT NOT NULL,
  departed_at     TEXT
);

CREATE TABLE IF NOT EXISTS announcements (
  id        TEXT PRIMARY KEY,
  title     TEXT NOT NULL,
  body      TEXT,
  posted_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_visitors_inside ON visitors(departed_at);

-- Ready-made report: free beds per room.
CREATE VIEW IF NOT EXISTS room_free_beds AS
SELECT r.id AS room_id, r.capacity, r.capacity - COUNT(o.student_id) AS free_beds
FROM rooms r LEFT JOIN room_occupants o ON o.room_id = r.id
GROUP BY r.id;
