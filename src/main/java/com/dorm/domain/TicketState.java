package com.dorm.domain;

import java.io.Serializable;

/**
 * State pattern (L8). Each state decides which transitions are legal,
 * so no if/else on a status string is needed anywhere.
 */
public interface TicketState extends Serializable {
    String name();

    default TicketState assign() { throw illegal("assign"); }
    default TicketState start()  { throw illegal("start"); }
    default TicketState resolve() { throw illegal("resolve"); }

    private IllegalStateException illegal(String action) {
        return new IllegalStateException("Cannot " + action + " a ticket that is " + name());
    }
}
