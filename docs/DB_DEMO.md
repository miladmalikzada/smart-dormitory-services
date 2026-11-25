# Database demonstration (5 minutes)

Goal: prove that the Java application really stores its data in a relational SQLite database,
that the data survives a restart, and that the database protects the business rules.

## Before the defence
```bash
mvn -q package -DskipTests        # optional: check that everything compiles
rm -rf data                       # start with a clean database
```

## Step 1 - Show the design (30 s)
Open `src/main/resources/schema.sql` and `docs/DATABASE.md` (ER diagram).
Say: "Six tables in third normal form, primary and foreign keys, CHECK rules, one view."

## Step 2 - Use the application, data goes into SQLite (2 min)
```bash
mvn -q exec:java -Dexec.args="--storage=sqlite --data=data"
```
Type these menu options (copy the printed ids, they are random):

| Menu | Input | Result |
|---|---|---|
| 1 | Ayan, 777 | prints `Registered S-xxxxxx` (copy it) |
| 2 | S-xxxxxx | assigned to a room |
| 15 | STAFF | role is STAFF |
| 5 | R101, Heater broken, high | prints `T-xxxxxx` |
| 6 / 7 | T-xxxxxx, Aibek / T-xxxxxx | ticket assigned, then started |
| 10 | Ali, S-xxxxxx | visitor arrives |
| 13 | Water, Off at 10 | announcement stored |
| 0 | | exit |

## Step 3 - Show what is inside the database (1 min)
```bash
python3 scripts/show-db.py
```
It prints every table, the reports (free beds, open tickets, visitors inside, joins) and then
**section 4: the database refuses wrong data** (unknown room, invalid status, capacity 0, second room for a student).
Alternative: install the VS Code extension **SQLite Viewer** and open `data/dorm.db`.

## Step 4 - Prove persistence (30 s)
Start the app again with the same command, choose `4` (list rooms), `9` (list tickets), `14` (announcements).
The data is still there. Then say: "The service layer did not change; only `--storage=sqlite` did."

## If time is short or the live demo fails
```bash
mvn -q exec:java -Dexec.args="--storage=sqlite --data=data" <<< "0"     # creates the empty database
python3 scripts/show-db.py data/dorm.db --sample                          # fills demo rows and shows everything
```

## Questions to expect
* **Why SQLite?** No server to install, real SQL and constraints, tests run offline.
* **Why six tables?** Residents are rows in `room_occupants`, not a list inside `rooms`: no repeated data (3NF).
* **How does Java reach the database?** Through the `Repository` interface; `SqlRepository` is a Template Method base class with five subclasses; only `infra` imports `java.sql`.
* **What if we change to PostgreSQL?** New repository classes behind the same interface; the service layer is unchanged.
