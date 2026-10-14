package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TaskApiTest {
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = Main.start(0);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + path);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .method(method, publisher)
                .header("Content-Type", "application/json; charset=utf-8")
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static JsonObject object(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private JsonArray list() throws Exception {
        HttpResponse<String> response = request("GET", "/api/tasks", null);
        assertEquals(200, response.statusCode());
        return JsonParser.parseString(response.body()).getAsJsonArray();
    }

    private JsonObject create(String title) throws Exception {
        HttpResponse<String> response = request("POST", "/api/tasks", "{\"title\": \"" + title + "\"}");
        assertEquals(201, response.statusCode());
        return object(response);
    }

    private static void assertError(HttpResponse<String> response, int status) {
        assertEquals(status, response.statusCode());
        assertEquals("application/json; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
        assertTrue(object(response).has("error"));
    }

    @Test
    void taskHasAllFields() throws Exception {
        JsonObject task = create("A");
        for (String field : new String[]{"id", "title", "done", "createdAt"}) {
            assertTrue(task.has(field), field);
        }
        assertDoesNotThrow(() -> Instant.parse(task.get("createdAt").getAsString()));
        assertEquals(task, list().get(0));
    }

    @Test
    void createsTaskWithCyrillicTitle() throws Exception {
        HttpResponse<String> response = request("POST", "/api/tasks", "{\"title\": \"Купить молоко\"}");
        assertEquals(201, response.statusCode());
        assertEquals("application/json; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
        JsonObject task = object(response);
        assertEquals("Купить молоко", task.get("title").getAsString());
        assertFalse(task.get("done").getAsBoolean());
        assertEquals("Купить молоко", list().get(0).getAsJsonObject().get("title").getAsString());
    }

    @Test
    void trimsTitle() throws Exception {
        assertEquals("Позвонить", create("  Позвонить  ").get("title").getAsString());
    }

    @Test
    void idsAreUnique() throws Exception {
        assertNotEquals(create("A").get("id").getAsLong(), create("B").get("id").getAsLong());
    }

    @Test
    void rejectsBlankTitleOnCreate() throws Exception {
        assertError(request("POST", "/api/tasks", "{\"title\": \"   \"}"), 400);
        assertEquals(0, list().size());
    }

    @Test
    void rejectsMissingTitleOnCreate() throws Exception {
        assertError(request("POST", "/api/tasks", "{}"), 400);
        assertEquals(0, list().size());
    }

    @Test
    void rejectsBlankTitleOnUpdate() throws Exception {
        JsonObject task = create("A");
        assertError(request("PUT", "/api/tasks/" + task.get("id"), "{\"title\": \"\"}"), 400);
        assertEquals(task, list().get(0));
    }

    @Test
    void rejectsSymbolOnlyTitle() throws Exception {
        assertError(request("POST", "/api/tasks", "{\"title\": \"+\"}"), 400);
        assertError(request("POST", "/api/tasks", "{\"title\": \"!!!\"}"), 400);
        assertEquals(0, list().size());
        JsonObject task = create("A");
        assertError(request("PUT", "/api/tasks/" + task.get("id"), "{\"title\": \"---\"}"), 400);
        assertEquals(task, list().get(0));
    }

    @Test
    void acceptsSymbolsNextToLetters() throws Exception {
        assertEquals("C++", create("C++").get("title").getAsString());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        assertError(request("POST", "/api/tasks", "{\"title\":"), 400);
        assertError(request("POST", "/api/tasks", "[1, 2]"), 400);
        JsonObject task = create("A");
        assertError(request("PUT", "/api/tasks/" + task.get("id"), "{\"done\":"), 400);
        assertEquals(task, list().get(0));
    }

    @Test
    void emptyList() throws Exception {
        HttpResponse<String> response = request("GET", "/api/tasks", null);
        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    void listIsOrderedByCreation() throws Exception {
        create("A");
        create("B");
        JsonArray tasks = list();
        assertEquals("A", tasks.get(0).getAsJsonObject().get("title").getAsString());
        assertEquals("B", tasks.get(1).getAsJsonObject().get("title").getAsString());
    }

    @Test
    void marksTaskDone() throws Exception {
        JsonObject task = create("A");
        HttpResponse<String> response = request("PUT", "/api/tasks/" + task.get("id"), "{\"done\": true}");
        assertEquals(200, response.statusCode());
        JsonObject updated = object(response);
        assertTrue(updated.get("done").getAsBoolean());
        assertEquals("A", updated.get("title").getAsString());
        assertEquals(task.get("id"), updated.get("id"));
        assertEquals(task.get("createdAt"), updated.get("createdAt"));
    }

    @Test
    void renamesTask() throws Exception {
        JsonObject task = create("A");
        request("PUT", "/api/tasks/" + task.get("id"), "{\"done\": true}");
        HttpResponse<String> response = request("PUT", "/api/tasks/" + task.get("id"), "{\"title\": \"Новое имя\"}");
        assertEquals(200, response.statusCode());
        JsonObject updated = object(response);
        assertEquals("Новое имя", updated.get("title").getAsString());
        assertTrue(updated.get("done").getAsBoolean());
    }

    @Test
    void deletesTask() throws Exception {
        JsonObject task = create("A");
        HttpResponse<String> response = request("DELETE", "/api/tasks/" + task.get("id"), null);
        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
        assertEquals(0, list().size());
    }

    @Test
    void missingTaskIsNotFound() throws Exception {
        assertError(request("PUT", "/api/tasks/999", "{\"done\": true}"), 404);
        assertError(request("DELETE", "/api/tasks/999", null), 404);
        assertError(request("DELETE", "/api/tasks/abc", null), 404);
    }

    @Test
    void unsupportedMethodIsRejected() throws Exception {
        assertError(request("DELETE", "/api/tasks", null), 405);
        assertError(request("GET", "/api/tasks/1", null), 405);
    }

    @Test
    void restartClearsTasks() throws Exception {
        create("A");
        server.stop(0);
        server = Main.start(0);
        assertEquals(0, list().size());
    }

    @Test
    void servesIndexPage() throws Exception {
        HttpResponse<String> response = request("GET", "/", null);
        assertEquals(200, response.statusCode());
        assertEquals("text/html; charset=utf-8", response.headers().firstValue("Content-Type").orElse(""));
        assertTrue(response.body().contains("<html"));
    }

    @Test
    void unknownPathIsNotFound() throws Exception {
        assertEquals(404, request("GET", "/nope", null).statusCode());
    }
}
