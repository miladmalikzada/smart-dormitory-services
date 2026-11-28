package com.dorm.service;

import com.dorm.domain.Role;
import com.dorm.domain.Room;

import java.util.List;

/**
 * Protection Proxy (same idea as SecuredStudentService): everybody may look at rooms,
 * but only an administrator may add a room, change its capacity or delete it.
 */
public class SecuredRoomAdminService implements RoomAdminOperations {
    private final RoomAdminOperations target;
    private final SessionContext session;

    public SecuredRoomAdminService(RoomAdminOperations target, SessionContext session) {
        this.target = target;
        this.session = session;
    }

    @Override public List<Room> list() { return target.list(); }
    @Override public Room get(String id) { return target.get(id); }
    @Override public Room create(String id, int capacity) { requireAdmin(); return target.create(id, capacity); }
    @Override public Room updateCapacity(String id, int capacity) { requireAdmin(); return target.updateCapacity(id, capacity); }
    @Override public void delete(String id) { requireAdmin(); target.delete(id); }

    private void requireAdmin() {
        if (session.role() != Role.ADMIN) throw new AccessDeniedException("Only an administrator can add, resize or delete rooms");
    }
}
