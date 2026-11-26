# ADR-0001: Four layers with inward dependencies

Status: accepted · Date: week 1

**Context:** The system has UI, business rules and storage; mixing them makes every change risky and tests slow.

**Options:** (A) one package; (B) layers domain/service/infra/ui with ArchUnit rules.

**Decision:** B. ui -> service -> domain; infra -> service/domain; only App wires concrete classes. Rules live in ArchitectureTest.

**Consequences:** More packages and interfaces (Repository, NotificationChannel), but each layer can be tested alone and the build fails if someone breaks a rule.
