package com.antest1.kcanotify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

// JsonElement.toString() without its slow number check. Before writing a number that is not a standard type
// (Integer, Long, ...), Gson matches it against a regex, and every number in a parsed tree is a
// LazilyParsedNumber, so toString() of a large response runs the regex tens of thousands of times.
// Those numbers are written as the parser read them, so the check is skipped for them.
public class KcaJson {
    // the same settings as JsonElement.toString(): lenient, no HTML escaping, nulls written
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    public static String toJson(JsonElement element) {
        StringWriter out = new StringWriter();
        gson.toJson(element, new ParsedNumberWriter(out));
        return out.toString();
    }

    private static class ParsedNumberWriter extends JsonWriter {
        ParsedNumberWriter(Writer out) {
            super(out);
        }

        @Override
        public JsonWriter value(Number value) throws IOException {
            if (value instanceof LazilyParsedNumber) return jsonValue(value.toString());
            return super.value(value);
        }
    }
}
