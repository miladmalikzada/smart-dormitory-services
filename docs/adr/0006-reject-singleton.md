# ADR-0006: Singleton rejected

Status: accepted · Date: week 11

**Context:** Shared objects (EventPublisher, CommandHistory) must exist once per application.

**Options:** (A) Singleton with static getInstance(); (B) create once in App and pass by constructor.

**Decision:** B. Singleton would be hidden global state, make tests share data and hide dependencies.

**Consequences:** Constructors take more parameters, but dependencies are visible and tests build fresh objects.
