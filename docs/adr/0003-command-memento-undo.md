# ADR-0003: Command + Memento for undo

Status: accepted · Date: week 6

**Context:** Room assignment must be undoable and the room must return to exactly the previous state.

**Options:** (A) write inverse logic in the service; (B) Command with Room.Memento; (C) copy whole Room objects.

**Decision:** B. AssignRoomCommand saves a Room.Memento before changing, undo restores it. CommandHistory is the invoker. C was rejected because it breaks identity and exposes internals.

**Consequences:** Undo history lives in memory only (lost on restart). Proven by RoomUndoTest.
