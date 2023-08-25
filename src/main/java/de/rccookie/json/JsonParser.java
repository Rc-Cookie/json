package de.rccookie.json;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

import de.rccookie.util.IterableIterator;

/**
 * Parser class to parse json formatted string data into json elements. The
 * parser also allows parsing multiple json values from a single source
 * which may be seperated using whitespaces. Instances can be obtained using
 * one of the {@code Json.getParser()} methods.
 *
 * @author RcCookie
 */
public class JsonParser implements IterableIterator<JsonElement>, AutoCloseable {

    /**
     * The string or reader to parse from.
     */
    private final JsonReader json;

    /**
     * Whether the parser has been closed;
     */
    private boolean closed = false;

    /**
     * Creates a new json parser over the given reader.
     *
     * @param reader The reader containing the json string
     */
    JsonParser(Reader reader) {
        json = new JsonReader(reader);
    }


    // ---------- Public API ----------

    /**
     * Closes this json parser and the underlying source. Subsequent calls
     * to {@link #hasNext()} will return {@code false} and calls to
     * {@link #next()} will result in an {@link IllegalStateException}
     * being thrown.
     */
    @Override
    public void close() {
        closed = true;
        json.close();
    }

    /**
     * Returns whether the parser is not yet closed and has more non-blank
     * content. Note that a return value of {@code true} does not necessarily
     * mean that the remaining content is in valid json syntax.
     *
     * @return Whether the source has more non-blank content to try and parse
     */
    @Override
    public boolean hasNext() {
        return !closed && !json.skipToContent().isEmpty();
    }

    /**
     * Parses and returns the next json value from the underlying source.
     *
     * @return The parsed json value wrapped in a {@link JsonElement}
     * @throws JsonParseException If the underlying source does not contain a
     *                            valid json string. Note that a black string
     *                            is <b>not</b> a valid json value
     * @throws UncheckedIOException If the underlying source produces an
     *                              {@link IOException}
     */
    @Override
    public JsonElement next() throws JsonParseException {
        if(closed) throw new IllegalStateException("Parser has been closed");
        if(!hasNext()) throw new JsonParseException("Blank string is not a valid json value", json);
        return JsonElement.wrap(parseNextValue());
    }


    // ---------- Internals ----------


    /**
     * Parses the next value from the source. The value must start immediately.
     *
     * @return The parsed value
     */
    private Object parseNextValue() {
        // No skipToContent needed, always called before
        if(json.skipIf("null")) return null;
        if(json.skipIf("true")) return true;
        if(json.skipIf("false")) return false;

        if(json.startsWith('{')) return parseNextStructure(true);
        if(json.startsWith('[')) return parseNextStructure(false);
        if(json.startsWith('"')) return parseNextString();
        return parseNextNumber();
    }

    /**
     * Parses the next json structure from the source. The structure must start
     * immediately, and it is assumed that the first character is the required one
     * to open an array or object, as specified.
     *
     * @param isObject {@code true} indicates a json object, {@code false} an array
     * @return The parsed json structure
     */
    private JsonStructure parseNextStructure(boolean isObject) {
        json.skip(); // Already checked in call condition
        char end = isObject ? '}' : ']';

        JsonStructure structure = isObject ? new JsonObject() : new JsonArray();
        if(json.skipToContent().skipIf(end))
            return structure;

        if(isObject) {
            do {
                String key = parseNextString();
                json.skipToContent();
                char first = json.read();
                if(first != ':')
                    throw new JsonParseException(':', first, json);
                json.skipToContent();
                Object value = parseNextValue();
                if(((JsonObject) structure).put(key, value) != null)
                    throw new JsonParseException("Duplicate key '" + key + "'", json);

                if (json.skipToContent().startsWith(','))
                    json.skip().skipWhitespaces();
                else if(!json.startsWith('}'))
                    throw new JsonParseException(",' or '}", json.peek(), json);
            } while(!json.skipToContent().endOfValue());
        }
        else {
            do {
                ((JsonArray) structure).add(parseNextValue());

                if (json.skipToContent().startsWith(','))
                    json.skip().skipWhitespaces();
                else if(!json.startsWith(']'))
                    throw new JsonParseException(",' or ']", json.peek(), json);
            } while(!json.skipToContent().endOfValue());
        }

        json.skipToContent().skipExpected(end);
        return structure;
    }

    /**
     * Parses the next number from the source. The number must start immediately.
     *
     * @return The parsed number
     */
    private Number parseNextNumber() {
        StringBuilder numberString = new StringBuilder();
        while(!json.endOfValue())
            numberString.append(json.read());
        if(numberString.length() == 0)
            // General message, if it's not a number it may have been supposed to be anything
            throw new JsonParseException("<value>", json.peekDescription(), json);
        String num = numberString.toString();

        if(num.contains(".") || num.contains("e") || num.contains("E"))
            try { return Double.parseDouble(num); } catch(NumberFormatException ignored) { }
        else
            try { return Long.  parseLong  (num); } catch(NumberFormatException ignored) { }

        throw new JsonParseException("Unexpected non-string value '" + num + "'", json);
    }

    /**
     * Parses the next string from the source. The string must start immediately.
     *
     * @return The parsed string
     */
    private String parseNextString() {
        json.skipExpected('"');
        StringBuilder string = new StringBuilder();

        while(!json.startsWith('"')) {
            char c = json.read();
            if(c == '\\') {
                c = json.read();
                if(c == 't') c = '\t';
                else if(c == 'b') c = '\b';
                else if(c == 'n') c = '\n';
                else if(c == 'r') c = '\r';
                else if(c == 'f') c = '\f';
                else if(c == 'u') c = (char) Integer.parseInt(json.read(4), 16);
                else if(c != '\\' && c != '/' && c != '"') throw new JsonParseException("Unknown escape sequence: '\\" + c + "'", json);
            }
            string.append(c);
        }

        json.skipExpected('"');
        return string.toString();
    }
}
