# ADR-0007: Visitor rejected

Status: accepted · Date: week 12

**Context:** Reports over rooms, tickets and visitors were considered.

**Options:** (A) Visitor over a common element hierarchy; (B) plain service methods.

**Decision:** B. Entities share no hierarchy and there is only one operation per type, so Visitor adds double dispatch without benefit (YAGNI, Lecture 7). Revisit if three or more report types over one structure appear.

**Consequences:** Adding a report means a new service method, which is acceptable at this size.
