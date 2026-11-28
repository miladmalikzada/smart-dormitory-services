package com.dorm.web;

import static org.junit.jupiter.api.Assertions.*;

import com.dorm.App;
import com.dorm.infra.EnvConfig;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The server must refuse bodies over 64 KB before they reach the API. */
class RequestSizeTest {
    @TempDir Path tmp;

    @Test void oversizedBodyIsRefused() throws Exception {
        Api api = App.wire("memory", tmp, new PrintStream(new ByteArrayOutputStream()),
                EnvConfig.of(Map.of())).api();
        WebServer server = new WebServer(api, 0);
        server.start();
        try {
            String big = "{\"x\":\"" + "a".repeat(WebServer.MAX_BODY + 10) + "\"}";
            HttpResponse<String> res = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + "/api/students"))
                            .POST(HttpRequest.BodyPublishers.ofString(big)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(413, res.statusCode());
            assertFalse(res.body().contains("Exception"));
        } finally {
            server.stop();
        }
    }
}
