package com.antest1.kcanotify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class KcaHandlerTest {
    private static JsonObject parse(String response) throws IOException {
        return KcaHandler.parseResponse(response.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void removesSvdataPrefix() throws IOException {
        assertEquals(JsonParser.parseString("{\"api_result\":1,\"api_data\":{\"api_id\":[1,2]}}"),
                parse("svdata={\"api_result\":1,\"api_data\":{\"api_id\":[1,2]}}"));
    }

    @Test
    public void parsesWithoutPrefix() throws IOException {
        assertEquals(JsonParser.parseString("{\"uri\":\"/kcsapi/api_port/port\"}"),
                parse("{\"uri\":\"/kcsapi/api_port/port\"}"));
    }

    @Test
    public void givesEmptyObjectForEmptyOrNonObjectResponse() throws IOException {
        assertEquals(new JsonObject(), parse(""));
        assertEquals(new JsonObject(), parse("[1,2]"));
        assertEquals(new JsonObject(), parse("svdata=1"));
    }

    @Test
    public void throwsForBrokenResponse() {
        // the service records these in the error log
        assertThrows(Exception.class, () -> parse("svdata={\"api_result\":"));
        assertThrows(Exception.class, () -> parse(" "));
    }
}
