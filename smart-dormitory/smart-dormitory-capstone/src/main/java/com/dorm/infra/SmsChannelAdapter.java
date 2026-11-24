package com.dorm.infra;

import com.dorm.service.NotificationChannel;

/** Adapter pattern (L4): makes the vendor API look like our NotificationChannel port. */
public class SmsChannelAdapter implements NotificationChannel {
    private static final int MAX_SMS_LENGTH = 160;
    private final LegacySmsGateway gateway;

    public SmsChannelAdapter(LegacySmsGateway gateway) { this.gateway = gateway; }

    @Override public String name() { return "sms"; }

    @Override public void send(String recipient, String message) {
        String text = message.length() > MAX_SMS_LENGTH ? message.substring(0, MAX_SMS_LENGTH) : message;
        gateway.sendText(recipient, text);
    }
}
