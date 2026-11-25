# Research and analysis

> Extend this document with sources you really read. Every claim about a book or website must be one you checked.

## 1. Research questions
1. What do dormitory services need, and which parts change often?
2. Which design patterns fit those changing parts, and which do not?
3. How should data be stored so that the design stays independent of the storage?

## 2. Domain analysis
| Stakeholder | Needs | Source of change |
|---|---|---|
| Student | A bed, quick repair of problems, news from the dormitory | New channels (SMS, e-mail, push) |
| Maintenance staff | A clear lifecycle for every request | New statuses (for example CLOSED) |
| Guard | Fast visitor registration, list of who is inside | Rules such as visiting hours |
| Administrator | Fair room allocation, announcements | Allocation rules change each year |

Main findings: (a) allocation rules, channels and statuses change often, so they must be easy to add;
(b) mistakes in room assignment must be reversible; (c) security rules must not be mixed with business rules.
Add here two or three real dormitory or hostel systems you reviewed, and what you took from them.

## 3. Problem-to-pattern analysis
| Problem found | Candidates | Chosen | Reason |
|---|---|---|---|
| Allocation rule changes | enum + switch, Strategy | Strategy | New rule = new class |
| Undo of room assignment | inverse logic, Command, Command + Memento | Command + Memento | Exact restore, no internals exposed |
| Ticket lifecycle | status string, enum, State | State | Illegal moves rejected by the state itself |
| Who may change tickets | checks in service, Proxy | Proxy | Service stays free of security code |
| Several delivery channels | direct calls, Observer | Observer + Factory Method + Decorator + Adapter | Add channels without editing services |
| Vendor SMS API | call SDK directly, Adapter | Adapter | Domain not tied to a vendor |
| Complex ticket object | long constructor, Builder | Builder | Validated, readable creation |
| Visitors still inside | filter in each caller, Iterator | Iterator | One traversal rule, list hidden |
| Repeated SQL flow | copy code, Template Method | Template Method | One query flow, five small subclasses |
| Singleton, Visitor, Facade | considered | rejected | See ADR-0006, 0007, 0008 |

## 4. Storage research
| Option | Pros | Cons | Used for |
|---|---|---|---|
| Memory | Fast, no setup | Lost on exit | Tests, demo |
| Serialised file | Simple, no library | Not queryable, not safe for concurrent use | Second implementation |
| SQLite (JDBC) | Real SQL, constraints, one file, offline | Single writer | Persistent mode |
| PostgreSQL / MySQL | Multi-user, scalable | Server needed, tests need network | Future work |

## 5. Method
Iterative development in four milestones; every design decision recorded as an ADR (`docs/adr`);
architecture protected by ArchUnit; behaviour protected by JUnit tests and a 60% coverage gate.

## 6. Risks
| Risk | Mitigation |
|---|---|
| Over-engineering | Pattern audit (ADR-0010), three rejected patterns |
| Storage lock-in | Repository interface, contract tests |
| Broken architecture over time | ArchUnit rules in CI |

## 7. References
1. Gamma, E., Helm, R., Johnson, R., Vlissides, J. *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley, 1994.
2. Freeman, E., Robson, E. *Head First Design Patterns*, 2nd ed. O'Reilly, 2020.
3. Fowler, M. *Patterns of Enterprise Application Architecture* (Repository, Unit of Work). Addison-Wesley, 2002.
4. Fowler, M. *Refactoring: Improving the Design of Existing Code*, 2nd ed. Addison-Wesley, 2018.
5. Martin, R. C. *Clean Architecture*. Prentice Hall, 2017.
6. Martin, R. C. *Agile Software Development: Principles, Patterns, and Practices*. Prentice Hall, 2002 (SOLID).
7. Meyer, B. *Object-Oriented Software Construction*. Prentice Hall, 1988 (Open/Closed principle).
8. SQLite documentation, sqlite.org/docs.html. Oracle JDBC tutorial, docs.oracle.com/javase/tutorial/jdbc.
9. ArchUnit User Guide, archunit.org. Refactoring.Guru design pattern catalogue, refactoring.guru/design-patterns.
