# ADR-0014: Only administrators manage rooms, and a new student can get a room at once

Status: accepted · Date: after v1.3.0

**Context:** Two things were unsatisfactory after the website was built. First, any signed-in role could add, resize or delete rooms, although the capacity of a dormitory is an administrator decision. Second, giving a new student a room took two steps (add the student, then press *Request room*), and it was easy to forget the second one.

**Options for who may manage rooms:** (A) leave it open; (B) check the role inside `RoomAdminService`; (C) a Protection Proxy, `SecuredRoomAdminService`, that implements the same `RoomAdminOperations` interface as the real service (the same idea as `SecuredStudentService` and `SecuredTicketService`, ADR-0004).

**Options for the automatic room:** (D) `StudentService` calls `RoomService` itself; (E) `Api` registers the student and then asks `RoomService.requestRoom(...)` when the form sets `assignRoom`.

**Decision:** C and E. The proxy keeps role checks out of the room rules. `RoomAdminService` still owns the safety rules (no shrinking below the residents, no deleting a room that has residents or repair tickets). For the automatic room, `Api` coordinates two use cases that stay independent: `StudentService` knows nothing about rooms, and the room is chosen by the existing allocation strategy (`LeastOccupiedStrategy`, so the room with the most free beds) as an undoable command.

**Consequences:** Staff and students can still look at rooms but get 403 when they try to change them (`SecuredRoomAdminServiceTest`, `ApiTest`). The two steps are not one database transaction: if no bed is free the student is still saved and the answer says so (`roomMessage`), so nothing is lost and the administrator can add a room and use *Request room*. To fill floor by floor instead, change one line in `App` to `FloorStrategy`. See UML diagram 14.
