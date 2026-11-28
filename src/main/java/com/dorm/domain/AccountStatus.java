package com.dorm.domain;

/** ACTIVE accounts can sign in. PENDING ones wait for the administrator. REJECTED ones were declined or switched off. */
public enum AccountStatus { ACTIVE, PENDING, REJECTED }
