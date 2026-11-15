package com.antest1.kcanotify;

import static org.junit.Assert.assertEquals;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

public class KcaJsonTest {
    // parses like KcaService
    private static final Gson gson = new GsonBuilder().setLenient().create();

    private static JsonElement parse(String json) {
        return gson.fromJson(json, JsonElement.class);
    }

    private static void assertSameAsToString(JsonElement element) {
        assertEquals(element.toString(), KcaJson.toJson(element));
    }

    @Test
    public void writesGameDataLikeToString() throws IOException {
        // the bundled api_start2 response (gzip); the working directory of unit tests is the module
        String text;
        try (InputStream in = new GZIPInputStream(new FileInputStream("src/main/assets/api_start2"))) {
            text = new String(ByteStreams.toByteArray(in), StandardCharsets.UTF_8);
        }
        JsonElement data = parse(text);
        String expected = data.toString();
        assertEquals(expected, KcaJson.toJson(data));

        // the shortest of a few runs each, to leave out JIT warm-up
        long toStringTime = Long.MAX_VALUE;
        long toJsonTime = Long.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            long startTime = System.nanoTime();
            data.toString();
            toStringTime = Math.min(toStringTime, System.nanoTime() - startTime);
            startTime = System.nanoTime();
            KcaJson.toJson(data);
            toJsonTime = Math.min(toJsonTime, System.nanoTime() - startTime);
        }
        System.out.printf("api_start2 (%d chars): toString %dms, KcaJson %dms%n", expected.length(),
                TimeUnit.NANOSECONDS.toMillis(toStringTime), TimeUnit.NANOSECONDS.toMillis(toJsonTime));

        // the service also writes parts of a response
        for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject().getAsJsonObject("api_data").entrySet()) {
            assertSameAsToString(entry.getValue());
        }
    }

    @Test
    public void writesParsedNumbersAsRead() {
        assertSameAsToString(parse("[0,-0,1,-1,1.5,1.50,-0.25,1e3,1E+3,2.5e-3,12345678901234567890]"));
    }

    @Test
    public void writesNumbersMadeInCode() {
        JsonObject object = new JsonObject();
        object.addProperty("int", 1);
        object.addProperty("long", 1L << 40);
        object.addProperty("double", 1.5);
        object.addProperty("nan", Double.NaN);
        assertSameAsToString(object);
    }

    @Test
    public void writesStringsLikeToString() {
        assertSameAsToString(parse("{\"escaped\":\"\\u6210\\u529f\",\"raw\":\"成功\",\"slash\":\"a\\/b\","
                + "\"html\":\"<&>='\",\"control\":\"\\\"\\\\\\n\\t\\u0001\\u2028\"}"));
    }

    @Test
    public void writesNullsAndEmptyValuesLikeToString() {
        assertSameAsToString(parse("{\"a\":null,\"b\":[null,{}],\"c\":[],\"d\":{},\"e\":true,\"f\":false,\"g\":\"\"}"));
    }
}
