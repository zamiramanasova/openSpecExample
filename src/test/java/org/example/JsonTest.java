package org.example;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {
    @Test
    void serializesTask() {
        Instant createdAt = Instant.parse("2026-10-14T10:15:30Z");
        JsonObject json = Json.GSON.toJsonTree(new Task(7, "Купить молоко", true, createdAt)).getAsJsonObject();
        assertEquals(7, json.get("id").getAsLong());
        assertEquals("Купить молоко", json.get("title").getAsString());
        assertTrue(json.get("done").getAsBoolean());
        assertEquals("2026-10-14T10:15:30Z", json.get("createdAt").getAsString());
    }

    @Test
    void roundTripsTask() {
        Task task = new Task(1, "A", false, Instant.parse("2026-10-14T10:15:30.123Z"));
        assertEquals(task, Json.GSON.fromJson(Json.GSON.toJson(task), Task.class));
    }
}
