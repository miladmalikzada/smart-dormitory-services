# Defence preparation

## 15-minute plan
1. Demo (5 min): register student, request room, undo, switch to STAFF, ticket lifecycle, announcement on two channels.
2. Walk-through (5 min): pick State, Command+Memento, Proxy. For each: smell, class diagram, what breaks without it.
3. Live change request (5 min demo): see below.

## Rehearse these change requests (each should need a NEW class)
| Request | Where it goes |
|---|---|
| "Add an e-mail channel" | `EmailChannelFactory` (+ adapter if a vendor API) in `infra`, one line in `App` |
| "Allocate by floor / gender / year of study" | new `RoomAllocationStrategy` class, one line in `App` (floor is already done: `FloorStrategy`, see `FloorStrategyTest`) |
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

## Two more questions, with answers
**Why can staff look at rooms but not change them, and where is that rule?**
In `SecuredRoomAdminService`, a Protection Proxy. It implements the same `RoomAdminOperations` interface as `RoomAdminService`, so `Api` cannot tell them apart. Reading is passed through; adding, resizing and deleting call `requireAdmin()` first and throw `AccessDeniedException`, which the API turns into 403. The room rules themselves (no deleting a room that has residents) stay in `RoomAdminService`, so there is no `if (role ...)` inside it. See ADR-0014 and `SecuredRoomAdminServiceTest`.

**How does a new staff or admin account get its rights, and what stops someone signing up as admin?**
A sign-up only creates a request. Students are `ACTIVE` at once; staff and admin start as `PENDING` and cannot sign in until an administrator approves them on the Accounts page (`AuthService.approve`). Nobody can approve or reject their own account, the number of waiting requests is capped at 50, and sign-ups are limited to 20 per minute. The owner account `admin` comes from `.env` at every start, so the owner cannot be locked out. See ADR-0013, UML diagrams 13 and 15, and `AuthLimitsTest`.
