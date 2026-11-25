package com.dorm.domain;

import java.io.Serializable;

public record Student(String id, String name, String phone) implements Entity, Serializable { }
