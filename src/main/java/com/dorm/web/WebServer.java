package com.dorm.web;

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
 * Security: strict Content-Security-Policy and other headers, 64 KB request limit, no CORS
 * (the page and the API share one origin, so other websites cannot call the API from a browser).
 */
public class WebServer {
    static final int MAX_BODY = 64 * 1024;
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8", "css", "text/css; charset=utf-8",
            "js", "application/javascript; charset=utf-8", "svg", "image/svg+xml", "ico", "image/x-icon",
            "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg", "webp", "image/webp");

    private final HttpServer server;
    private final Api api;

    public WebServer(Api api, int port) throws IOException {
        this.api = api;
        // No executor is set on purpose: the JDK then handles requests one after another. Api and the services
        // are not thread-safe and rely on that (see RoomRequestConcurrencyTest). A thread pool would need locks first.
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", this::handleApi);
        server.createContext("/", this::handleStatic);
    }

    public void start() { server.start(); }
    public void stop() { server.stop(0); }
    public int port() { return server.getAddress().getPort(); }

    private void handleApi(HttpExchange ex) throws IOException {
        try (ex) {
            byte[] raw = ex.getRequestBody().readNBytes(MAX_BODY + 1);
            if (raw.length > MAX_BODY) {
                send(ex, 413, "application/json; charset=utf-8", "{\"error\":\"Request too large\"}".getBytes(StandardCharsets.UTF_8));
                return;
            }
            Api.Response r = api.handleRequest(ex.getRequestMethod(), ex.getRequestURI().getPath(),
                    new String(raw, StandardCharsets.UTF_8), ex.getRequestHeaders().getFirst("Authorization"));
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
        var h = ex.getResponseHeaders();
        h.set("Content-Type", type);
        h.set("Cache-Control", "no-store");
        h.set("X-Content-Type-Options", "nosniff");
        h.set("X-Frame-Options", "DENY");
        h.set("Referrer-Policy", "no-referrer");
        h.set("Content-Security-Policy", "default-src 'self'; style-src 'self'; script-src 'self'; "
                + "img-src 'self' data:; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }
}
