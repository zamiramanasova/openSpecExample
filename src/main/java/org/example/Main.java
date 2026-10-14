package org.example;

import com.sun.net.httpserver.HttpServer;

import java.io.Console;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Main {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException {
        setUpConsoleEncoding();
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
        HttpServer running = server;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.stop(0);
            System.out.println("Сервер остановлен.");
        }));
        System.out.println("Менеджер задач запущен: http://localhost:" + server.getAddress().getPort() + "/");
        System.out.println("Чтобы остановить сервер, нажмите Ctrl+C.");
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

    /**
     * Чтобы кириллица в консоли не ломалась: в терминале пишем в его кодировке (в Windows это cp866),
     * без терминала (IDE, вывод в файл) — в UTF-8. По умолчанию Java 17 на Windows пишет в cp1251.
     */
    private static void setUpConsoleEncoding() {
        Console console = System.console();
        Charset charset = console != null ? console.charset() : StandardCharsets.UTF_8;
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, charset));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, charset));
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
