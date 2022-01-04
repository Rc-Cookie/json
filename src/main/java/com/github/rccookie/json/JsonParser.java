package com.github.rccookie.json;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

/**
 * Internal class to handle json parsing.
 */
final class JsonParser {

    private final JsonReader json;

    JsonParser(Reader reader) {
        json = new JsonReader(reader);
    }



    void close() {
        json.close();
    }

    JsonStructure parseNextStructure() {
        json.skipToContent();
        if(removeIfStart("null")) return null;

        char first = json.peek();
        if(first == '{') return parseNextStructure(true);
        if(first == '[') return parseNextStructure(false);
        throw new JsonParseException("'{' or '['", first, json);
    }

    private JsonStructure parseNextStructure(boolean isObject) {
        json.skip();
        char end = isObject ? '}' : ']';

        JsonStructure structure = isObject ? new JsonObject() : new JsonArray();
        if(json.skipToContent().startsWith(end)) {
            json.skip();
            return structure;
        }

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
            } while(noEndOfLine(json.skipToContent().peek()));
        }
        else {
            do {
                ((JsonArray) structure).add(parseNextValue());

                if (json.skipToContent().startsWith(','))
                    json.skip().skipWhitespaces();
                else if(!json.startsWith(']'))
                    throw new JsonParseException(",' or ']", json.peek(), json);
            } while(noEndOfLine(json.skipToContent().peek()));
        }

        json.skipToContent();
        deleteFirstWithExpectation(isObject ? '}' : ']');
        return structure;
    }



    private Object parseNextValue() {
        json.skipToContent();
        if(removeIfStart("null")) return null;
        if(removeIfStart("true")) return true;
        if(removeIfStart("false")) return false;

        if(json.startsWith('{')) return parseNextStructure(true);
        if(json.startsWith('[')) return parseNextStructure(false);
        if(json.startsWith('"')) return parseNextString();

        StringBuilder numberString = new StringBuilder();
        while(noEndOfLine(json.peek()))
            numberString.append(json.read());
        if(numberString.length() == 0)
            throw new JsonParseException("<value>", "<end of line>", json);
        String num = numberString.toString();

        try { return Integer.parseInt   (num); } catch(NumberFormatException ignored) { }
        try { return Long.   parseLong  (num); } catch(NumberFormatException ignored) { }
        try { return Float.  parseFloat (num); } catch(NumberFormatException ignored) { }
        try { return Double. parseDouble(num); } catch(NumberFormatException ignored) { }

        throw new JsonParseException("Unexpected non-string value '" + num + "'", json);
    }

    private String parseNextString() {
        deleteFirstWithExpectation('"');

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

        deleteFirstWithExpectation('"');
        return string.toString();
    }

    private void deleteFirstWithExpectation(char expected) {
        char found = json.read();
        if(found != expected) throw new JsonParseException(expected, found, json);
    }

    private boolean removeIfStart(String possibleStart) {
        if(json.startsWith(possibleStart)) {
            json.skip(possibleStart.length());
            return true;
        }
        return false;
    }

    private static boolean noEndOfLine(char c) {
        return c != '\n' && c != ' ' && c != ',' && c != ']' && c != '}' && c != '/'; // '/' -> Comment
    }


    public static void main(String[] args) throws Exception {
        HttpsURLConnection con = (HttpsURLConnection) new URL("https://create.kahoot.it/rest/kahoots").openConnection();
        con.setRequestMethod("GET");

        String params = Parse.map("{'username':'somekindamailadress@gmail.com','password':'Hoernchen06!','grant_type':'password'}")
                .entrySet().stream().map(Object::toString).collect(Collectors.joining("&"));
        con.setRequestProperty("Content-Type", "application/json");

//        con.setDoOutput(true);
//        DataOutputStream out = new DataOutputStream(con.getOutputStream());
//        out.writeBytes(params);
//        out.close();

        System.out.println(Json.load(new InputStreamReader(con.getInputStream())));
    }
}
