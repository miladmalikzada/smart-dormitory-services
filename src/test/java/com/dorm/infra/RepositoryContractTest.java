package com.dorm.infra;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.domain.Repository;
import com.dorm.domain.Room;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Both implementations must behave the same, so the service layer never notices the switch. */
class RepositoryContractTest {
    @TempDir Path tmp;

    private void checkContract(Repository<Room> repo) {
        Room r = new Room("R1", 2);
        r.assign("S1");
        repo.save(r);
        assertEquals(1, repo.findAll().size());
        assertEquals(java.util.List.of("S1"), repo.findById("R1").orElseThrow().occupants());
        assertTrue(repo.findById("nope").isEmpty());
        repo.delete("R1");
        assertTrue(repo.findAll().isEmpty());
    }

    @Test void inMemory() { checkContract(new InMemoryRepository<>()); }

    @Test void file() { checkContract(new FileRepository<>(tmp.resolve("rooms.bin"))); }

    @Test void fileRepositoryKeepsDataBetweenInstances() {
        new FileRepository<Room>(tmp.resolve("r.bin")).save(new Room("R9", 4));
        assertEquals(4, new FileRepository<Room>(tmp.resolve("r.bin")).findById("R9").orElseThrow().capacity());
    }
}
