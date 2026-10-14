package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Отдаёт страницу менеджера задач из classpath:/static. */
public class StaticHandler implements HttpHandler {
    private static final String INDEX = "/static/index.html";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if (!path.equals("/") && !path.equals("/index.html")) {
                Json.sendError(exchange, 404, "Ресурс не найден");
                return;
            }
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Allow", "GET");
                Json.sendError(exchange, 405, "Метод не поддерживается");
                return;
            }
            byte[] bytes;
            try (InputStream in = StaticHandler.class.getResourceAsStream(INDEX)) {
                if (in == null) {
                    Json.sendError(exchange, 404, "Ресурс не найден");
                    return;
                }
                bytes = in.readAllBytes();
            }
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}
