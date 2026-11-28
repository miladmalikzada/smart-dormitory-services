# ADR-0002: Strategy for room allocation

Status: accepted · Date: week 5

**Context:** Which room a new resident gets is a policy that dormitory management changes (fill one room first, or spread students).

**Options:** (A) enum + switch in RoomService; (B) Strategy interface, one class per rule.

**Decision:** B: FirstAvailableStrategy and LeastOccupiedStrategy (FloorStrategy was added later as a third rule, with no change to RoomService). A new rule is a new class (Open/Closed), each tested alone.

**Consequences:** Two small extra classes; App chooses the strategy in one line.
