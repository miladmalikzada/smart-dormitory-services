-- Demo data for the SQLite database. Run after the app has created data/dorm.db once:
--   sqlite3 data/dorm.db < docs/sample_data.sql
INSERT OR IGNORE INTO rooms(id, capacity) VALUES ('R101', 2), ('R102', 2), ('R201', 3);
-- Identity numbers (iin, national_id_number, passport_number) are left NULL here on purpose: the database
-- stores them encrypted, so they can only be added through the application (it holds the key from .env).
INSERT OR IGNORE INTO students(id, first_name, last_name, date_of_birth, gender, nationality, phone, email, faculty, course_year) VALUES
  ('S-demo1', 'Ayan', 'Nurlan', '2004-05-17', 'MALE',   'Kazakhstani', '+77001112233', 'ayan@uni.kz', 'Information Systems', 2),
  ('S-demo2', 'Dana', 'Serik',  '2003-11-02', 'FEMALE', 'Kazakhstani', '+77004445566', 'dana@uni.kz', 'Law', 3);
INSERT OR IGNORE INTO room_occupants(room_id, student_id, position) VALUES ('R101', 'S-demo1', 0);
INSERT OR IGNORE INTO tickets(id, room_id, description, priority, status, technician, created_at) VALUES
  ('T-demo1', 'R101', 'Heater not working', 'HIGH', 'OPEN', NULL, '2026-11-20T09:00:00');
INSERT OR IGNORE INTO visitors(id, visitor_name, host_student_id, arrived_at, departed_at) VALUES
  ('V-demo1', 'Ali', 'S-demo1', '2026-11-20T18:00:00', NULL);

-- Try these queries:
SELECT * FROM room_free_beds;
SELECT id, room_id, priority, status FROM tickets WHERE status <> 'RESOLVED';
SELECT visitor_name, host_student_id FROM visitors WHERE departed_at IS NULL;
