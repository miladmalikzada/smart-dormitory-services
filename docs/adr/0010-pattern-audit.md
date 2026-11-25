# ADR-0010: Pattern audit (checkpoint, week 7)

Status: accepted · Date: week 7

**Context:** DRAFT - the team must re-run this audit on its own code and update the text. Smells checked: switch on status, security checks in services, direct vendor calls, global state.

**Options:** (A) keep every pattern considered; (B) remove those without a real problem.

**Decision:** B. Facade and Visitor were removed from the plan (ADR-0007, ADR-0008). Every remaining pattern in the register maps to a smell and a test.

**Consequences:** Fewer classes; the register lists only patterns that earn their place.
