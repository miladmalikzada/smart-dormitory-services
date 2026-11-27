# ADR-0011: SQLite database behind the Repository interface

Status: accepted · Date: week 13

**Context:** The course asks for a second persistence option and the professor expects a real database design. Tests must not need a server or the network.

**Options:** (A) PostgreSQL/MySQL; (B) SQLite through JDBC; (C) keep only serialised files.

**Decision:** B. A normalised schema (see docs/DATABASE.md) with primary keys, foreign keys and CHECK constraints. `SqlRepository` is a Template Method base class; five small subclasses map rows to entities. Services still see only `Repository`. Files stay as a simple third option.

**Consequences:** One new runtime dependency (sqlite-jdbc, listed in the README). Domain classes got `restore(...)` factory methods so stored objects can be rebuilt. Switching storage is `--storage=sqlite`; no service code changes.
