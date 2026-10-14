package org.example;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;

public class Main {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException {
        int port = readPort();
        HttpServer server;
        try {
            server = start(port);
        } catch (BindException e) {
            System.err.println("Не удалось запустить сервер: порт " + port + " занят. "
                    + "Освободите порт или задайте другой через переменную окружения PORT.");
            System.exit(1);
            return;
        }
        System.out.println("Менеджер задач запущен: http://localhost:" + server.getAddress().getPort() + "/");
    }

    /** Собирает приложение и запускает сервер; {@code port = 0} — любой свободный порт. */
    public static HttpServer start(int port) throws IOException {
        TaskService service = new TaskService(new TaskRepository());
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(TaskHandler.BASE, new TaskHandler(service));
        server.createContext("/", new StaticHandler());
        server.start();
        return server;
    }

    private static int readPort() {
        String value = System.getenv("PORT");
        if (value == null || value.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            System.err.println("Некорректное значение PORT: " + value + ", используется " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}
