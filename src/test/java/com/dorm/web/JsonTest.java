package com.dorm.web;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {
    @Test void writesObjectsListsAndEscapes() {
        String json = Json.write(Map.of("text", "a \"quoted\"\nline"));
        assertEquals("{\"text\":\"a \\\"quoted\\\"\\nline\"}", json);
        assertEquals("[1,true,null,\"x\"]", Json.write(java.util.Arrays.asList(1, true, null, "x")));
    }

    @Test void parsesNestedJson() {
        Map<String, Object> m = Json.parseObject("{\"name\":\"Ayan\",\"n\":3,\"ok\":true,\"tags\":[\"a\",\"b\"],\"x\":null}");
        assertEquals("Ayan", m.get("name"));
        assertEquals(3, ((Number) m.get("n")).intValue());
        assertEquals(Boolean.TRUE, m.get("ok"));
        assertEquals(List.of("a", "b"), m.get("tags"));
        assertNull(m.get("x"));
    }

    @Test void roundTripKeepsUnicodeAndEscapes() {
        Map<String, Object> m = Json.parseObject("{\"t\":\"caf\\u00e9 \\\"x\\\"\"}");
        assertEquals("café \"x\"", m.get("t"));
    }

    @Test void blankBodyIsAnEmptyObjectAndBadJsonIsRejected() {
        assertTrue(Json.parseObject("  ").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("{bad"));
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("[1,2]"));
    }
}
