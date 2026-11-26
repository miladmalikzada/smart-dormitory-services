# Defence preparation

## 15-minute plan
1. Demo (5 min): register student, request room, undo, switch to STAFF, ticket lifecycle, announcement on two channels.
2. Walk-through (5 min): pick State, Command+Memento, Proxy. For each: smell, class diagram, what breaks without it.
3. Live change request (5 min demo): see below.

## Rehearse these change requests (each should need a NEW class)
| Request | Where it goes |
|---|---|
| "Add an e-mail channel" | `EmailChannelFactory` (+ adapter if a vendor API) in `infra`, one line in `App` |
| "Allocate by floor / gender / year of study" | new `RoomAllocationStrategy` class, one line in `App` |
| "Add a CLOSED state after RESOLVED" | new state class, change `Resolved.close()` only (State: only the neighbouring state changes) |
| "Log every command" | new `DormEventListener` or a `Command` decorator |
| "Visitors must leave by 22:00 - report overdue" | new method on `VisitorLog`/iterator `OverdueIterator` |

Practise: branch -> test first -> class -> PR -> merge, in under 20 minutes, in VS Code.

## Questions to be able to answer without notes
* Why is `Room.Memento` an inner class with a private constructor?
* Why does `SecuredTicketService` implement the same interface as `TicketService`?
* Why is `ChannelFactory.create()` final?
* Why is there no Singleton? Where are the objects created?
* What does `ArchitectureTest` stop a teammate from doing?
* Which patterns did you consider and reject, and why?
