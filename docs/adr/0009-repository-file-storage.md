# ADR-0009: Repository port with in-memory and file implementations

Status: accepted · Date: week 13

**Context:** Tests must not use a database, but users need data to survive restarts.

**Options:** (A) JDBC/SQLite now; (B) Repository interface with InMemoryRepository and FileRepository (Java serialisation).

**Decision:** B. Both pass the same contract test (RepositoryContractTest). App switches with --storage=memory|file; services are unchanged.

**Consequences:** Java serialisation is simple but not for production or concurrent use; a SQLite implementation can be added behind the same interface.
