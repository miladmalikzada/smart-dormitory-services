package com.dorm.service;

import com.dorm.domain.Room;

import java.util.List;

/** Subject interface shared by the real room service and its access-control Proxy. */
public interface RoomAdminOperations {
    List<Room> list();
    Room get(String id);
    Room create(String id, int capacity);
    Room updateCapacity(String id, int capacity);
    void delete(String id);
}
