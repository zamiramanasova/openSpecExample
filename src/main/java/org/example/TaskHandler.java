package org.example;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Маршрутизация /api/tasks и /api/tasks/{id}: HTTP -> сервис -> статус и JSON. */
public class TaskHandler implements HttpHandler {
    static final String BASE = "/api/tasks";

    private final TaskService service;

    public TaskHandler(TaskService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            try {
                route(exchange);
            } catch (ValidationException e) {
                Json.sendError(exchange, 400, e.getMessage());
            } catch (NotFoundException e) {
                Json.sendError(exchange, 404, e.getMessage());
            } catch (RuntimeException e) {
                e.printStackTrace();
                Json.sendError(exchange, 500, "Внутренняя ошибка сервера");
            }
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals(BASE) || path.equals(BASE + "/")) {
            switch (method) {
                case "GET" -> Json.send(exchange, 200, service.list());
                case "POST" -> {
                    JsonObject body = readObject(exchange);
                    Json.send(exchange, 201, service.create(stringField(body, "title")));
                }
                default -> methodNotAllowed(exchange, "GET, POST");
            }
            return;
        }

        if (!path.startsWith(BASE + "/")) {
            throw new NotFoundException("Ресурс не найден");
        }
        if (!method.equals("PUT") && !method.equals("DELETE")) {
            methodNotAllowed(exchange, "PUT, DELETE");
            return;
        }
        long id = parseId(path.substring(BASE.length() + 1));
        if (method.equals("PUT")) {
            JsonObject body = readObject(exchange);
            Json.send(exchange, 200, service.update(id, stringField(body, "title"), booleanField(body, "done")));
        } else {
            service.delete(id);
            Json.sendNoContent(exchange);
        }
    }

    private static void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowed);
        Json.sendError(exchange, 405, "Метод не поддерживается");
    }

    private static long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new NotFoundException("Задача " + raw + " не найдена");
        }
    }

    private static JsonObject readObject(HttpExchange exchange) throws IOException {
        String text;
        try (InputStream in = exchange.getRequestBody()) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        JsonElement element;
        try {
            element = JsonParser.parseString(text);
        } catch (JsonParseException e) {
            throw new ValidationException("Тело запроса не является корректным JSON");
        }
        if (!element.isJsonObject()) {
            throw new ValidationException("Тело запроса должно быть JSON-объектом");
        }
        return element.getAsJsonObject();
    }

    /** {@code null}, если поле не передано; явный {@code null} считается пустой строкой. */
    private static String stringField(JsonObject body, String name) {
        if (!body.has(name)) {
            return null;
        }
        JsonElement value = body.get(name);
        if (value.isJsonNull()) {
            return "";
        }
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isString()) {
            throw new ValidationException("Поле " + name + " должно быть строкой");
        }
        return primitive.getAsString();
    }

    private static Boolean booleanField(JsonObject body, String name) {
        if (!body.has(name)) {
            return null;
        }
        JsonElement value = body.get(name);
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isBoolean()) {
            throw new ValidationException("Поле " + name + " должно быть true или false");
        }
        return primitive.getAsBoolean();
    }
}
