package org.example;

import java.time.Instant;

public record Task(long id, String title, boolean done, Instant createdAt) {
}
