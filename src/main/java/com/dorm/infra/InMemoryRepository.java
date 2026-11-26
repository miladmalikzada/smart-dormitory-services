package com.dorm.infra;

import com.dorm.domain.Entity;
import com.dorm.domain.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryRepository<T extends Entity> implements Repository<T> {
    private final Map<String, T> store = new LinkedHashMap<>();

    @Override public void save(T entity) { store.put(entity.id(), entity); }
    @Override public Optional<T> findById(String id) { return Optional.ofNullable(store.get(id)); }
    @Override public List<T> findAll() { return new ArrayList<>(store.values()); }
    @Override public void delete(String id) { store.remove(id); }
}
