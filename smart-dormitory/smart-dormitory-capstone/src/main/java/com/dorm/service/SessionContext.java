package com.dorm.service;

import com.dorm.domain.Role;

/** Who is using the application right now. Passed explicitly, not a global. */
public class SessionContext {
    private Role role;

    public SessionContext(Role role) { this.role = role; }

    public Role role() { return role; }
    public void switchTo(Role role) { this.role = role; }
}
