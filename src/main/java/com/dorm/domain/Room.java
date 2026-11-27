package com.dorm.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** A dormitory room. It is also the Originator of the Memento pattern (L13). */
public class Room implements Entity, Serializable {
    private final String id;
    private final int capacity;
    private final List<String> occupants = new ArrayList<>();

    public Room(String id, int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
        this.id = id;
        this.capacity = capacity;
    }

    @Override public String id() { return id; }
    public int capacity() { return capacity; }
    public List<String> occupants() { return List.copyOf(occupants); }
    public int freeBeds() { return capacity - occupants.size(); }
    public boolean isFull() { return freeBeds() == 0; }
    public boolean houses(String studentId) { return occupants.contains(studentId); }

    public void assign(String studentId) {
        if (isFull()) throw new IllegalStateException("Room " + id + " is full");
        if (houses(studentId)) throw new IllegalStateException(studentId + " already lives in " + id);
        occupants.add(studentId);
    }

    public void vacate(String studentId) {
        if (!occupants.remove(studentId)) throw new IllegalStateException(studentId + " does not live in " + id);
    }

    /** Memento: opaque snapshot, only Room can read it. */
    public Memento save() { return new Memento(new ArrayList<>(occupants)); }

    public void restore(Memento m) {
        occupants.clear();
        occupants.addAll(m.occupants);
    }

    public static final class Memento implements Serializable {
        private final List<String> occupants;
        private Memento(List<String> occupants) { this.occupants = occupants; }
    }
}
