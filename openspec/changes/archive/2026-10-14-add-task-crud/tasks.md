# Tasks

## 1. Настройка проекта

- [x] 1.1 Добавить в `pom.xml` зависимость `com.google.code.gson:gson`, тестовую `org.junit.jupiter:junit-jupiter`, `maven-surefire-plugin` (3.x) и `exec-maven-plugin` с `mainClass = org.example.Main`; проверить, что `mvn -q compile` проходит
- [x] 1.2 Создать каталоги `src/main/resources/static/` и `src/test/java/org/example/`; проверить, что `mvn -q test` проходит (тестов пока нет)

## 2. Модель и бизнес-логика

- [x] 2.1 Создать `record Task(long id, String title, boolean done, Instant createdAt)`; проверить компиляцией
- [x] 2.2 Реализовать `TaskRepository` (`LinkedHashMap` + `AtomicLong`, `synchronized`): `findAll`, `findById`, `save`, `delete`; проверить юнит-тестом `TaskRepositoryTest` (уникальные id, порядок создания, удаление)
- [x] 2.3 Реализовать `ValidationException` и `NotFoundException`, а также `TaskService`: `list`, `create(title)` с trim и проверкой на пустоту, `update(id, title?, done?)` с частичным обновлением, `delete(id)`; проверить `TaskServiceTest` по сценариям спеки `task-management` (создание, обрезка пробелов, пустой/отсутствующий title, частичное обновление, неизменность `id`/`createdAt`, 404-случаи)

## 3. HTTP API

- [x] 3.1 Настроить Gson с адаптером `Instant` ↔ ISO-8601 и общий helper записи JSON-ответа (`application/json; charset=utf-8`) и ошибок `{"error": ...}`; проверить юнит-тестом сериализацию `Task`
- [x] 3.2 Реализовать `TaskHandler` для `/api/tasks` и `/api/tasks/{id}`: GET/POST/PUT/DELETE, разбор `PUT` через `JsonObject`, маппинг `ValidationException`/битого JSON → 400, `NotFoundException`/нечислового id → 404, прочих методов → 405; проверить
- [x] 3.3 Переписать `Main`: порт из `PORT` (по умолчанию 8080, `0` — свободный порт для тестов), сборка repository → service → handler, понятное сообщение при занятом порте; вынести запуск в метод, возвращающий `HttpServer`, чтобы его можно было стартовать в тестах
- [x] 3.4 Написать интеграционный тест `TaskApiTest` (сервер на свободном порту + `java.net.http.HttpClient`), покрывающий все сценарии `specs/task-management`, включая кириллицу в `title`; проверить, что `mvn test` зелёный

## 4. Веб-страница

- [x] 4.1 Реализовать `StaticHandler`: `GET /` отдаёт `static/index.html` из classpath с `text/html; charset=utf-8`, неизвестный путь → 404; проверить тестом в `TaskApiTest`, что `GET /` возвращает 200 и HTML
- [x] 4.2 Создать `index.html`: загрузка и отображение списка, сообщение о пустом списке, стиль выполненных задач, вывод заголовков через `textContent`; проверить вручную, открыв `http://localhost:8080/` после `mvn compile exec:java`
- [x] 4.3 Добавить в страницу создание (кнопка и Enter, очистка поля), переключатель `done`, переименование двойным кликом (Enter — сохранить, Esc — отмена), удаление и показ ошибок API; проверить вручную каждый сценарий `specs/task-web-ui`, включая пустой заголовок и остановленный сервер

## 4a. Доработка валидации

- [x] 4a.1 Отклонять `title` без единой буквы или цифры (`+`, `!!!`, `---`) ответом `400`; покрыть тестами в `TaskServiceTest` (каждый ASCII-символ пунктуации по отдельности, комбинации символов, допустимые `C++`, `1`) и `TaskApiTest` (POST и PUT); проверить, что `mvn test` зелёный

## 5. Документация и интеграция

- [x] 5.1 Добавить `README.md` с описанием API и командой запуска `mvn compile exec:java`; проверить, что команда из README запускает приложение
- [x] 5.2 Финальная проверка: `mvn clean test` зелёный, `openspec validate add-task-crud` без ошибок, полный ручной прогон CRUD в браузере
