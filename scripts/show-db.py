#!/usr/bin/env python3
"""Shows the SQLite database of Smart Dormitory Services (for the defence demo).

Usage:  python3 scripts/show-db.py [path/to/dorm.db] [--sample]      (default: data/dorm.db)
        --sample  first adds the demo rows from docs/sample_data.sql (safe to repeat)
Needs only Python 3 (its sqlite3 module is built in). It never changes your data.
"""
import sqlite3
import sys
from pathlib import Path

args = [a for a in sys.argv[1:] if not a.startswith("--")]
load_sample = "--sample" in sys.argv
path = Path(args[0] if args else "data/dorm.db")
if not path.exists():
    sys.exit(f"{path} not found. Start the app once with:  mvn -q exec:java -Dexec.args=\"--storage=sqlite --data=data\"")

con = sqlite3.connect(path)
con.execute("PRAGMA foreign_keys = ON")
if load_sample:
    con.executescript(Path(__file__).resolve().parent.parent.joinpath("docs/sample_data.sql").read_text(encoding="utf-8"))
    con.commit()
    print("Demo rows loaded from docs/sample_data.sql")


def table(title, sql, params=()):
    cur = con.execute(sql, params)
    cols = [c[0] for c in cur.description]
    rows = cur.fetchall()
    print(f"\n=== {title} ({len(rows)} rows) ===")
    if not rows:
        print("(empty)")
        return
    width = [max(len(str(x)) for x in [c] + [r[i] for r in rows]) for i, c in enumerate(cols)]
    line = "  ".join(c.ljust(width[i]) for i, c in enumerate(cols))
    print(line)
    print("-" * len(line))
    for r in rows:
        print("  ".join(str("" if v is None else v).ljust(width[i]) for i, v in enumerate(r)))


def attempt(label, sql, params=()):
    try:
        con.execute(sql, params)
        con.rollback()
        print(f"  [!] {label}: was ACCEPTED (unexpected)")
    except sqlite3.IntegrityError as e:
        con.rollback()
        print(f"  [OK] {label}\n       database says: {e}")


print("SMART DORMITORY SERVICES - DATABASE DEMO")
print(f"File: {path.resolve()}")

table("1. Tables and views", "SELECT type, name FROM sqlite_master WHERE type IN ('table','view') AND name NOT LIKE 'sqlite_%' ORDER BY type DESC, name")
table("2. Table students (personal data; the three identity columns are ENCRYPTED)",
      "SELECT id, first_name, last_name, date_of_birth, gender, nationality, "
      "substr(iin, 1, 18) || CASE WHEN iin IS NULL THEN '' ELSE '...' END AS iin_encrypted, "
      "substr(passport_number, 1, 18) || CASE WHEN passport_number IS NULL THEN '' ELSE '...' END AS passport_encrypted, "
      "faculty, course_year FROM students")
for name in ["rooms", "room_occupants", "tickets", "visitors", "announcements"]:
    table(f"2. Table {name}", f"SELECT * FROM {name}")

table("3a. Report: free beds per room (view)", "SELECT * FROM room_free_beds")
table("3b. Report: unresolved tickets", "SELECT id, room_id, priority, status, technician FROM tickets WHERE status <> 'RESOLVED' ORDER BY priority")
table("3c. Report: tickets per status", "SELECT status, COUNT(*) AS tickets FROM tickets GROUP BY status")
table("3d. Report: who lives where (JOIN)", "SELECT o.room_id, s.first_name || ' ' || s.last_name AS student, s.phone FROM room_occupants o JOIN students s ON s.id = o.student_id ORDER BY o.room_id, o.position")
table("3e. Report: visitors still inside", "SELECT v.visitor_name, s.first_name || ' ' || s.last_name AS host, v.arrived_at FROM visitors v JOIN students s ON s.id = v.host_student_id WHERE v.departed_at IS NULL")

print("\n=== 4. The database protects the rules (nothing is saved) ===")
attempt("Ticket for a room that does not exist", "INSERT INTO tickets(id,room_id,description,priority,status,created_at) VALUES ('X','NO-ROOM','test','LOW','OPEN','2026-01-01T00:00:00')")
attempt("Ticket with an invalid status", "INSERT INTO tickets(id,room_id,description,priority,status,created_at) SELECT 'X', id, 'test','LOW','BROKEN','2026-01-01T00:00:00' FROM rooms LIMIT 1")
attempt("Room with capacity 0", "INSERT INTO rooms(id, capacity) VALUES ('BAD', 0)")
row = con.execute("SELECT o.student_id, o.room_id FROM room_occupants o LIMIT 1").fetchone()
if row:
    other = con.execute("SELECT id FROM rooms WHERE id <> ? LIMIT 1", (row[1],)).fetchone()
    if other:
        attempt(f"Student {row[0]} placed in a second room", "INSERT INTO room_occupants(room_id, student_id, position) VALUES (?,?,9)", (other[0], row[0]))
print("\nNote: iin, national_id_number and passport_number look like random text on purpose.")
print("They are encrypted with AES-256-GCM; only the app (with the key from .env) can read them.")
print("\nDone. The data above is exactly what the Java application stored.")
