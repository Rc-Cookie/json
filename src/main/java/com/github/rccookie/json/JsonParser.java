package com.github.rccookie.json;

/**
 * Internal class to handle json parsing.
 */
final class JsonParser {

    private final JsonStringBuilder json;

    JsonParser(String jsonString) {
        json = new JsonStringBuilder(jsonString);
    }



    JsonStructure parseNextStructure() {
        json.stripToContent();
        if(removeIfStart("null")) return null;

        char first = json.first();
        if(first == '{') return parseNextStructure(true);
        if(first == '[') return parseNextStructure(false);
        throw new JsonParseException("'{' or '['", first, json);
    }

    private JsonStructure parseNextStructure(boolean isObject) {
        json.deleteFirst();
        char end = isObject ? '}' : ']';

        JsonStructure structure = isObject ? new JsonObject() : new JsonArray();
        if(json.stripToContent().startsWith(end)) {
            json.deleteFirst();
            return structure;
        }

        if(isObject) {
            do {
                String key = parseNextString();
                json.stripToContent();
                char first = json.popFirst();
                if(first != ':')
                    throw new JsonParseException(':', first, json);
                json.stripToContent();
                Object value = parseNextValue();
                if(((JsonObject) structure).put(key, value) != null)
                    throw new JsonParseException("Duplicate key '" + key + "'", json);

                if (json.stripToContent().startsWith(','))
                    json.deleteFirst().stripLeading();
                else if(!json.startsWith('}'))
                    throw new JsonParseException(",' or '}", json.first(), json);
            } while(noEndOfLine(json.stripToContent().first()));
        }
        else {
            do {
                ((JsonArray) structure).add(parseNextValue());

                if (json.stripToContent().startsWith(','))
                    json.deleteFirst().stripLeading();
                else if(!json.startsWith(']'))
                    throw new JsonParseException(",' or ']", json.first(), json);
            } while(noEndOfLine(json.stripToContent().first()));
        }

        json.stripToContent();
        deleteFirstWithExpectation(isObject ? '}' : ']');
        return structure;
    }



    private Object parseNextValue() {
        json.stripToContent();
        if(removeIfStart("null")) return null;
        if(removeIfStart("true")) return true;
        if(removeIfStart("false")) return false;

        if(json.startsWith('{')) return parseNextStructure(true);
        if(json.startsWith('[')) return parseNextStructure(false);
        if(json.startsWith('"')) return parseNextString();

        StringBuilder numberString = new StringBuilder();
        while(noEndOfLine(json.first()))
            numberString.append(json.popFirst());
        if(numberString.length() == 0)
            throw new JsonParseException("<value>", "<end of line>", json);
        String num = numberString.toString();

        try { return Byte.   parseByte  (num); } catch(NumberFormatException ignored) { }
        try { return Short.  parseShort (num); } catch(NumberFormatException ignored) { }
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
            char c = json.popFirst();
            if(c == '\\') {
                c = json.popFirst();
                if(c == 't') c = '\t';
                else if(c == 'b') c = '\b';
                else if(c == 'n') c = '\n';
                else if(c == 'r') c = '\r';
                else if(c == 'f') c = '\f';
                else if(c != '\\' && c != '"') throw new JsonParseException("Unknown escape sequence: '\\" + c + "'", json);
            }
            string.append(c);
        }

        deleteFirstWithExpectation('"');
        return string.toString();
    }

    private void deleteFirstWithExpectation(char expected) {
        char found = json.popFirst();
        if(found != expected) throw new JsonParseException(expected, found, json);
    }

    private boolean removeIfStart(String possibleStart) {
        if(json.startsWith(possibleStart)) {
            json.delete(possibleStart.length());
            return true;
        }
        return false;
    }

    private static boolean noEndOfLine(char c) {
        return c != '\n' && c != ' ' && c != ',' && c != ']' && c != '}' && c != '/'; // '/' -> Comment
    }
}
