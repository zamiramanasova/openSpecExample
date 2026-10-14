package org.example;

import java.time.Instant;
import java.util.List;

/** Бизнес-правила задач; ничего не знает об HTTP. */
public class TaskService {
    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public List<Task> list() {
        return repository.findAll();
    }

    public Task create(String title) {
        return repository.save(new Task(0, normalizeTitle(title), false, Instant.now()));
    }

    /** Частичное обновление: {@code null} означает «поле не передано». */
    public synchronized Task update(long id, String title, Boolean done) {
        Task current = find(id);
        String newTitle = title != null ? normalizeTitle(title) : current.title();
        boolean newDone = done != null ? done : current.done();
        return repository.save(new Task(current.id(), newTitle, newDone, current.createdAt()));
    }

    public void delete(long id) {
        if (!repository.delete(id)) {
            throw notFound(id);
        }
    }

    private Task find(long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Заголовок задачи не может быть пустым");
        }
        if (title.codePoints().noneMatch(Character::isLetterOrDigit)) {
            throw new ValidationException("Заголовок задачи должен содержать хотя бы одну букву или цифру");
        }
        return title.strip();
    }

    private static NotFoundException notFound(long id) {
        return new NotFoundException("Задача " + id + " не найдена");
    }
}
