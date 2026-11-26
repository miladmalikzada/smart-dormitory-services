package com.dorm.web;

import com.dorm.domain.Role;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP adapter: serves the website (files in resources/web) and forwards /api/* to {@link Api}.
 * Uses the JDK's built-in server. With no executor set, requests run one after another,
 * which keeps the single database connection and the role switching safe.
 */
public class WebServer {
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8", "css", "text/css; charset=utf-8",
            "js", "application/javascript; charset=utf-8", "svg", "image/svg+xml", "ico", "image/x-icon");

    private final HttpServer server;
    private final Api api;

    public WebServer(Api api, int port) throws IOException {
        this.api = api;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", this::handleApi);
        server.createContext("/", this::handleStatic);
    }

    public void start() { server.start(); }
    public void stop() { server.stop(0); }
    public int port() { return server.getAddress().getPort(); }

    private void handleApi(HttpExchange ex) throws IOException {
        try (ex) {
            if (ex.getRequestMethod().equals("OPTIONS")) { send(ex, 204, "text/plain", new byte[0]); return; }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Api.Response r;
            try {
                Role role = Role.valueOf(ex.getRequestHeaders().getFirst("X-Role") == null
                        ? "STUDENT" : ex.getRequestHeaders().getFirst("X-Role").toUpperCase());
                r = api.handle(ex.getRequestMethod(), ex.getRequestURI().getPath(), body, role);
            } catch (IllegalArgumentException e) {
                r = new Api.Response(400, Map.of("error", "Invalid X-Role header or request"));
            }
            send(ex, r.status(), "application/json; charset=utf-8", Json.write(r.body()).getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleStatic(HttpExchange ex) throws IOException {
        try (ex) {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            if (path.contains("..")) { send(ex, 400, "text/plain", "Bad path".getBytes()); return; }
            try (InputStream in = WebServer.class.getResourceAsStream("/web" + path)) {
                if (in == null) { send(ex, 404, "text/plain", "Not found".getBytes()); return; }
                String ext = path.substring(path.lastIndexOf('.') + 1);
                send(ex, 200, TYPES.getOrDefault(ext, "application/octet-stream"), in.readAllBytes());
            }
        }
    }

    private void send(HttpExchange ex, int status, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Role");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        if (status == 204) { ex.sendResponseHeaders(204, -1); return; }
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }
}
