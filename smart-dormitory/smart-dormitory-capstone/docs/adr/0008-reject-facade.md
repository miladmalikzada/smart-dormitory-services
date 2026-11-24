# ADR-0008: Facade rejected

Status: accepted · Date: week 12

**Context:** ConsoleMenu uses four services; a DormitoryFacade was considered.

**Options:** (A) Facade over all services; (B) UI calls the services directly.

**Decision:** B. Each service is already small and cohesive; a facade would only forward calls and become a god class.

**Consequences:** The menu has four constructor dependencies. Revisit if a second UI (web) needs the same orchestration.
