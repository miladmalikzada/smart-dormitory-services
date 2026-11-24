package com.dorm.infra;

import com.dorm.domain.Entity;
import com.dorm.domain.Repository;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Second Repository implementation: one Java-serialised file per entity type. */
public class FileRepository<T extends Entity & Serializable> implements Repository<T> {
    private final Path file;

    public FileRepository(Path file) { this.file = file; }

    @Override public void save(T entity) {
        Map<String, T> data = read();
        data.put(entity.id(), entity);
        write(data);
    }

    @Override public Optional<T> findById(String id) { return Optional.ofNullable(read().get(id)); }

    @Override public List<T> findAll() { return new ArrayList<>(read().values()); }

    @Override public void delete(String id) {
        Map<String, T> data = read();
        data.remove(id);
        write(data);
    }

    @SuppressWarnings("unchecked")
    private Map<String, T> read() {
        if (!Files.exists(file)) return new LinkedHashMap<>();
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (Map<String, T>) in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private void write(Map<String, T> data) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
                out.writeObject(data);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
