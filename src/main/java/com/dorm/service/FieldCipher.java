package com.dorm.service;

/** Encrypts single text values before they are stored. Implemented in infra (AES-256-GCM). */
public interface FieldCipher {
    String encrypt(String plain);
    String decrypt(String stored);
}
