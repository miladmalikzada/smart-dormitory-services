# ADR-0004: Protection Proxy for ticket access

Status: accepted · Date: week 9

**Context:** Only staff may assign, start or resolve tickets; students may only report.

**Options:** (A) role checks inside TicketService; (B) Proxy implementing the same interface.

**Decision:** B: SecuredTicketService wraps TicketService and checks SessionContext. The real service has no security code.

**Consequences:** UI must use the TicketOperations interface; roles are checked in one place. Tested in ProxyTest.
