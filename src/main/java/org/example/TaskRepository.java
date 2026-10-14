package org.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory хранилище задач; порядок вставки совпадает с порядком создания. */
public class TaskRepository {
    private final Map<Long, Task> tasks = new LinkedHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public synchronized List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    public synchronized Optional<Task> findById(long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    /** Сохраняет задачу; задача с {@code id == 0} считается новой и получает новый id. */
    public synchronized Task save(Task task) {
        Task stored = task.id() == 0
                ? new Task(nextId.getAndIncrement(), task.title(), task.done(), task.createdAt())
                : task;
        tasks.put(stored.id(), stored);
        return stored;
    }

    public synchronized boolean delete(long id) {
        return tasks.remove(id) != null;
    }
}
