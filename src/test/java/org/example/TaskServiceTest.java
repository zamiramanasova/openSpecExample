package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {
    private final TaskService service = new TaskService(new TaskRepository());

    @Test
    void createsTask() {
        Task task = service.create("Купить молоко");
        assertNotEquals(0, task.id());
        assertEquals("Купить молоко", task.title());
        assertFalse(task.done());
        assertNotNull(task.createdAt());
    }

    @Test
    void trimsTitle() {
        assertEquals("Позвонить", service.create("  Позвонить  ").title());
    }

    @Test
    void rejectsBlankOrMissingTitle() {
        assertThrows(ValidationException.class, () -> service.create("   "));
        assertThrows(ValidationException.class, () -> service.create(""));
        assertThrows(ValidationException.class, () -> service.create(null));
        assertTrue(service.list().isEmpty());
    }

    @Test
    void rejectsEverySingleAsciiSymbol() {
        for (char c = '!'; c <= '~'; c++) {
            if (Character.isLetterOrDigit(c)) {
                continue;
            }
            String title = String.valueOf(c);
            assertThrows(ValidationException.class, () -> service.create(title), "title: " + title);
        }
        assertTrue(service.list().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"!!!", "---", "...", "@#$%", "+ - * /", "  +  ", "«»", "—", "№", "\t+\n"})
    void rejectsTitleWithoutLettersOrDigits(String title) {
        assertThrows(ValidationException.class, () -> service.create(title));
        assertTrue(service.list().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"C++", "1", "Купить +", "Задача №1", "!важно!", "ё"})
    void acceptsSymbolsNextToLettersOrDigits(String title) {
        assertEquals(title, service.create(title).title());
    }

    @Test
    void rejectsSymbolOnlyTitleOnUpdateWithoutChanges() {
        Task task = service.create("A");
        assertThrows(ValidationException.class, () -> service.update(task.id(), "+", true));
        assertEquals(task, service.list().get(0));
    }

    @Test
    void updatesOnlyDone() {
        Task task = service.create("A");
        Task updated = service.update(task.id(), null, true);
        assertTrue(updated.done());
        assertEquals("A", updated.title());
    }

    @Test
    void updatesOnlyTitle() {
        Task task = service.create("A");
        service.update(task.id(), null, true);
        Task updated = service.update(task.id(), " Новое имя ", null);
        assertEquals("Новое имя", updated.title());
        assertTrue(updated.done());
    }

    @Test
    void updateKeepsIdAndCreatedAt() {
        Task task = service.create("A");
        Task updated = service.update(task.id(), "B", true);
        assertEquals(task.id(), updated.id());
        assertEquals(task.createdAt(), updated.createdAt());
    }

    @Test
    void rejectsBlankTitleOnUpdateWithoutChanges() {
        Task task = service.create("A");
        assertThrows(ValidationException.class, () -> service.update(task.id(), "", true));
        assertEquals(task, service.list().get(0));
    }

    @Test
    void missingTaskIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.update(999, "X", null));
        assertThrows(NotFoundException.class, () -> service.delete(999));
    }

    @Test
    void deletesTask() {
        Task task = service.create("A");
        service.delete(task.id());
        assertTrue(service.list().isEmpty());
    }
}
