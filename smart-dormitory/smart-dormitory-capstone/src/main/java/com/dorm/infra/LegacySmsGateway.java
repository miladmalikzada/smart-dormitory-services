package com.dorm.infra;

import java.io.PrintStream;

/** Stand-in for a vendor SDK whose API we cannot change. */
public class LegacySmsGateway {
    private final PrintStream out;

    public LegacySmsGateway(PrintStream out) { this.out = out; }

    public void sendText(String phoneNumber, String text) {
        out.println("  [SMS gateway] to=" + phoneNumber + " text=" + text);
    }
}
