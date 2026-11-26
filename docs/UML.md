# UML diagrams

All diagrams are Mermaid, so GitHub draws them directly. In VS Code use the *Markdown Mermaid* extension.

## 1. Use case diagram
```mermaid
flowchart LR
  Student([Student]); Staff([Staff]); Guard([Guard]); Admin([Administrator])
  subgraph System[Smart Dormitory Services]
    UC1("Register / request room")
    UC2("Undo room request")
    UC3("Report problem")
    UC4("Assign, start, resolve ticket")
    UC5("Record visitor arrival and departure")
    UC6("List visitors inside")
    UC7("Post announcement")
    UC8("Receive notification")
  end
  Student --> UC1 & UC2 & UC3 & UC8
  Staff --> UC4
  Guard --> UC5 & UC6
  Admin --> UC7 & UC4
```

## 2. Class diagram: domain and state (State, Memento, Builder, Iterator)
```mermaid
classDiagram
  class Entity {
    <<interface>>
    +id() String
  }
  class Room {
    -capacity int
    -occupants List
    +assign(studentId)
    +save() Memento
    +restore(Memento)
  }
  class Memento {
    -occupants List
  }
  class MaintenanceTicket {
    -state TicketState
    +assignTo(t)
    +start()
    +resolve()
    +builder() Builder
  }
  class Builder {
    +id()
    +roomId()
    +description()
    +priority()
    +build()
  }
  class TicketState {
    <<interface>>
    +assign()
    +start()
    +resolve()
  }
  class Open
  class Assigned
  class InProgress
  class Resolved
  class VisitorEntry {
    +depart(time)
    +isInside() boolean
  }
  class VisitorLog {
    +iterator()
    +inside() Iterable
  }
  Entity <|.. Room
  Entity <|.. MaintenanceTicket
  Entity <|.. VisitorEntry
  Room *-- Memento
  MaintenanceTicket --> TicketState
  TicketState <|.. Open
  TicketState <|.. Assigned
  TicketState <|.. InProgress
  TicketState <|.. Resolved
  MaintenanceTicket ..> Builder
  VisitorLog o-- VisitorEntry
```

## 3. Class diagram: services and patterns (Command, Strategy, Proxy, Observer, Factory Method, Decorator, Adapter)
```mermaid
classDiagram
  class RoomService {
    +requestRoom(studentId)
    +undoLast()
  }
  class RoomAllocationStrategy {
    <<interface>>
    +choose(rooms) Optional
  }
  class FirstAvailableStrategy
  class LeastOccupiedStrategy
  class Command {
    <<interface>>
    +execute()
    +undo()
  }
  class AssignRoomCommand
  class CommandHistory {
    +execute(c)
    +undoLast()
  }
  class TicketOperations {
    <<interface>>
  }
  class TicketService
  class SecuredTicketService
  class EventPublisher {
    +subscribe(l)
    +publish(e)
  }
  class DormEventListener {
    <<interface>>
    +onEvent(e)
  }
  class NotificationListener
  class AuditLogListener
  class NotificationChannel {
    <<interface>>
    +send(to, msg)
  }
  class ChannelFactory {
    <<abstract>>
    #createChannel()
    +create()
  }
  class TimestampedChannel
  class ConsoleChannel
  class SmsChannelAdapter
  class LegacySmsGateway
  RoomService --> RoomAllocationStrategy
  RoomAllocationStrategy <|.. FirstAvailableStrategy
  RoomAllocationStrategy <|.. LeastOccupiedStrategy
  RoomService --> CommandHistory
  CommandHistory o-- Command
  Command <|.. AssignRoomCommand
  TicketOperations <|.. TicketService
  TicketOperations <|.. SecuredTicketService
  SecuredTicketService --> TicketOperations : delegates
  EventPublisher o-- DormEventListener
  DormEventListener <|.. NotificationListener
  DormEventListener <|.. AuditLogListener
  NotificationListener --> NotificationChannel
  NotificationChannel <|.. TimestampedChannel
  NotificationChannel <|.. ConsoleChannel
  NotificationChannel <|.. SmsChannelAdapter
  TimestampedChannel --> NotificationChannel : wraps
  SmsChannelAdapter --> LegacySmsGateway : adapts
  ChannelFactory ..> TimestampedChannel : creates
```

## 4. Class diagram: persistence (Repository, Template Method)
```mermaid
classDiagram
  class Repository~T~ {
    <<interface>>
    +save(t)
    +findById(id)
    +findAll()
    +delete(id)
  }
  class InMemoryRepository
  class FileRepository
  class SqlRepository {
    <<abstract>>
    #table()
    #map(row)
    #upsert(t)
  }
  class Students
  class Rooms
  class Tickets
  class Visitors
  class Announcements
  Repository <|.. InMemoryRepository
  Repository <|.. FileRepository
  Repository <|.. SqlRepository
  SqlRepository <|-- Students
  SqlRepository <|-- Rooms
  SqlRepository <|-- Tickets
  SqlRepository <|-- Visitors
  SqlRepository <|-- Announcements
```

## 5. Sequence diagram: request a room and undo (Strategy + Command + Memento + Observer)
```mermaid
sequenceDiagram
  actor S as Student
  participant UI as ConsoleMenu
  participant RS as RoomService
  participant ST as Strategy
  participant CH as CommandHistory
  participant CMD as AssignRoomCommand
  participant R as Room
  participant EP as EventPublisher
  S->>UI: request room
  UI->>RS: requestRoom(studentId)
  RS->>ST: choose(rooms)
  ST-->>RS: room R101
  RS->>CH: execute(command)
  CH->>CMD: execute()
  CMD->>R: save() (Memento)
  CMD->>R: assign(studentId)
  CMD->>EP: publish(ROOM_ASSIGNED)
  S->>UI: undo
  UI->>RS: undoLast()
  RS->>CH: undoLast()
  CH->>CMD: undo()
  CMD->>R: restore(Memento)
  CMD->>EP: publish(ROOM_ASSIGNMENT_UNDONE)
```

## 6. Sequence diagram: announcement reaches all channels (Observer + Factory Method + Decorator + Adapter)
```mermaid
sequenceDiagram
  actor A as Administrator
  participant AS as AnnouncementService
  participant EP as EventPublisher
  participant NL as NotificationListener
  participant C1 as TimestampedChannel(Console)
  participant C2 as TimestampedChannel(SmsAdapter)
  participant G as LegacySmsGateway
  A->>AS: post(title, body)
  AS->>EP: publish(ANNOUNCEMENT_POSTED)
  EP->>NL: onEvent()
  NL->>C1: send()
  NL->>C2: send()
  C2->>G: sendText()
```

## 7. State diagram: maintenance ticket (State)
```mermaid
stateDiagram-v2
  [*] --> OPEN: create
  OPEN --> ASSIGNED: assign(technician)
  ASSIGNED --> IN_PROGRESS: start
  IN_PROGRESS --> RESOLVED: resolve
  RESOLVED --> [*]
```

## 8. Component / layer diagram
```mermaid
flowchart TB
  UI[ui] --> SERVICE[service] --> DOMAIN[domain]
  INFRA[infra] --> SERVICE
  INFRA --> DOMAIN
  INFRA --> DB[("SQLite / file")]
  APP["App: composition root"] -.creates.-> UI & SERVICE & INFRA
```

The entity-relationship diagram is in [DATABASE.md](DATABASE.md).
