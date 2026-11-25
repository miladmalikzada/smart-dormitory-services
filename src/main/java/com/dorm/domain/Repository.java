package com.dorm.domain;

import java.util.List;
import java.util.Optional;

/** Persistence port (L14). Services depend on this interface only. */
public interface Repository<T extends Entity> {
    void save(T entity);
    Optional<T> findById(String id);
    List<T> findAll();
    void delete(String id);
}
