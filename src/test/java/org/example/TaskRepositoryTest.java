package org.example;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskRepositoryTest {
    private final TaskRepository repository = new TaskRepository();

    private Task saveNew(String title) {
        return repository.save(new Task(0, title, false, Instant.now()));
    }

    @Test
    void assignsUniqueIds() {
        Task a = saveNew("A");
        Task b = saveNew("B");
        assertNotEquals(0, a.id());
        assertNotEquals(a.id(), b.id());
    }

    @Test
    void keepsCreationOrder() {
        saveNew("A");
        saveNew("B");
        saveNew("C");
        assertEquals(List.of("A", "B", "C"), repository.findAll().stream().map(Task::title).toList());
    }

    @Test
    void replacingKeepsPosition() {
        Task a = saveNew("A");
        saveNew("B");
        repository.save(new Task(a.id(), "A2", true, a.createdAt()));
        assertEquals(List.of("A2", "B"), repository.findAll().stream().map(Task::title).toList());
    }

    @Test
    void deletesTask() {
        Task a = saveNew("A");
        assertTrue(repository.delete(a.id()));
        assertTrue(repository.findById(a.id()).isEmpty());
        assertTrue(repository.findAll().isEmpty());
        assertFalse(repository.delete(a.id()));
    }
}
