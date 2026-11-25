# Pattern register

| # | Pattern (category) | Lecture | Problem in the project | Key classes / test | Alternative considered |
|---|---|---|---|---|---|
| 1 | Factory Method (Creational) | L3 | Every notification channel must be created and decorated the same way | `ChannelFactory`, `ConsoleChannelFactory`, `SmsChannelFactory` / `AdapterAndDecoratorTest`, `ExtensionPointTest` | `new` in `App` + manual wrapping: easy to forget the decorator |
| 2 | Builder (Creational) | L9 | Ticket has required and optional fields; constructor with 5 args is error-prone | `MaintenanceTicket.Builder` / `TicketBuilderTest` | Telescoping constructors |
| 3 | Adapter (Structural) | L4 | Vendor SMS API (`sendText(phone,text)`) differs from our `NotificationChannel` | `SmsChannelAdapter`, `LegacySmsGateway` / `AdapterAndDecoratorTest` | Call the gateway from the service: couples domain to the vendor |
| 4 | Decorator (Structural) | L4 | Every channel needs a timestamp; subclassing each channel would multiply classes | `TimestampedChannel` / `AdapterAndDecoratorTest` | Subclass per channel |
| 5 | Proxy – protection (Structural) | L11 | Only staff may change tickets; checks would pollute `TicketService` | `SecuredTicketService`, `SessionContext` / `ProxyTest` | `if (role …)` inside the service |
| 6 | Observer (Behavioral) | L5 | Services must trigger notifications and audit without knowing the receivers | `EventPublisher`, `NotificationListener`, `AuditLogListener` / `ObserverTest` | Services call channels directly |
| 7 | Command (Behavioral) | L5 | Room assignment must be undoable | `Command`, `CommandHistory`, `AssignRoomCommand` / `RoomUndoTest` | Reverse-engineer undo with ad-hoc code in the service |
| 8 | Memento (Behavioral) | L13 | Undo must restore the room *exactly* without exposing its internals | `Room.Memento`, `Room.save/restore` / `RoomUndoTest` | Copy the whole `Room`: breaks encapsulation and identity |
| 9 | State (Behavioral) | L8 | Ticket status was a string checked in several places | `TicketState`, `TicketStates.*` / `TicketStateTest` | Status enum + switch: no behaviour per state |
| 10 | Strategy (Behavioral) | L8 | Room-choice rule will change (fill first / spread out) | `RoomAllocationStrategy`, `FirstAvailableStrategy`, `LeastOccupiedStrategy` / `AllocationStrategyTest` | Enum + switch in `RoomService` |
| 11 | Iterator (Behavioral) | L6 | "Who is still inside?" without exposing the list or repeating filter loops | `VisitorLog`, inner `InsideIterator` / `VisitorLogIteratorTest` | `stream().filter` in each caller |
| 12 | Template Method (Behavioral) | L6 | Five SQL repositories repeat the same find/list/delete flow | `SqlRepository`, `SqliteRepositories.*` / `SqliteRepositoryTest` | Copy the JDBC code five times |

Category count: Creational 2, Structural 3, Behavioral 7 (requirement: 2 / 2 / 3).

## Combinations (L7)
* **Command + Memento** – the command takes a Memento before acting; `undo()` restores it. (ADR-0003)
* **Factory Method + Decorator** – the factory's final `create()` always wraps the product in the decorator. (ADR-0005)

## Rejected patterns
| Pattern | Why rejected | ADR |
|---|---|---|
| Singleton | Hidden global state; `App` already gives one instance of everything by wiring | ADR-0006 |
| Visitor | Only one structure and no second operation over it; a method is enough (YAGNI) | ADR-0007 |
| Facade | UI talks to four small services; a facade would only forward calls | ADR-0008 |
