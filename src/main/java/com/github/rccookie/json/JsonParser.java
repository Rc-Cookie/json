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
        removeCommentAndStrip();
        if(startsWithThenRemove("null")) return null;

        char first = json.getAndDeleteFirst();
        final boolean isObject;
        if(first == '{') isObject = true;
        else if(first == '[') isObject = false;
        else throw new JsonParseException("<structure start>", first);

        char end = isObject ? '}' : ']';

        JsonStructure structure = isObject ? new JsonObject() : new JsonArray();
        if(removeCommentAndStrip().startsWith(end)) {
            json.deleteFirst();
            return structure;
        }

        if(isObject) {
            do {
                String key = parseNextString();
                removeCommentAndStrip();
                if((first = json.getAndDeleteFirst()) != ':')
                    throw new JsonParseException(':', first);
                removeCommentAndStrip();
                Object value = parseNextValue();
                if(((JsonObject) structure).put(key, value) != null)
                    throw new JsonParseException("Duplicate key '" + key + "'");

                if (removeCommentAndStrip().startsWith(','))
                    json.deleteFirst().stripLeading();
            } while(noEndOfLine(removeCommentAndStrip().first()));
        }
        else {
            do {
                ((JsonArray) structure).add(parseNextValue());

                if (removeCommentAndStrip().startsWith(','))
                    json.deleteFirst().stripLeading();
            } while(noEndOfLine(removeCommentAndStrip().first()));
        }

        removeCommentAndStrip();
        deleteFirstWithExpectation(end);
        return structure;
    }



    private Object parseNextValue() {
        removeCommentAndStrip();
        if(startsWithThenRemove("null")) return null;
        if(startsWithThenRemove("true")) return true;
        if(startsWithThenRemove("false")) return false;

        if(json.startsWith('{') || json.startsWith('[')) return parseNextStructure();
        if(json.startsWith('"')) return parseNextString();

        StringBuilder numberString = new StringBuilder();
        while(noEndOfLine(json.first()))
            numberString.append(json.getAndDeleteFirst());
        if(numberString.length() == 0)
            throw new JsonParseException("<value>", "<end of line>");
        String num = numberString.toString();

        // Ordered in use-count, and floating point at the back to prevent unnecessary
        // memory allocation: Don't take ints as longs, but most byte-size values are
        // probably supposed to be ints. And who even ever uses 'short'?
        try {
            return Integer.parseInt(num);
        } catch(NumberFormatException ignored) { }
        try {
            return Long.parseLong(num);
        } catch(NumberFormatException ignored) { }
        try {
            return Byte.parseByte(num);
        } catch(NumberFormatException ignored) { }
        try {
            return Short.parseShort(num);
        } catch(NumberFormatException ignored) { }
        try {
            return Double.parseDouble(num);
        } catch(NumberFormatException ignored) { }
        try {
            return Float.parseFloat(num);
        } catch(NumberFormatException ignored) { }

        throw new JsonParseException("Unexpected non-string value '" + num + "'");
    }

    private String parseNextString() {
        deleteFirstWithExpectation('"');

        StringBuilder string = new StringBuilder();
        while(!json.startsWith('"')) {
            char c = json.getAndDeleteFirst();
            if(c == '\\') {
                c = json.getAndDeleteFirst();
                if(c == 't') c = '\t';
                else if(c == 'b') c = '\b';
                else if(c == 'n') c = '\n';
                else if(c == 'r') c = '\r';
                else if(c == 'f') c = '\f';
                else if(c != '\\' && c != '"') throw new JsonParseException("Unknown character: '\\" + c + "'");
            }
            string.append(c);
        }

        deleteFirstWithExpectation('"');
        return string.toString();
    }

    private JsonStringBuilder removeCommentAndStrip() {
        if(json.stripLeading().startsWith("//")) {
            int index = json.indexOf('\n');
            if(index == -1) throw new JsonParseException("Reached end of line during comment");
            json.delete(index + 1);
            return removeCommentAndStrip();
        }
        if(json.startsWith("/*")) {
            int index = json.indexOf("*/");
            if(index == -1) throw new JsonParseException("Reached end of line during comment");
            json.delete(index + 2);
            return removeCommentAndStrip();
        }
        return json.stripLeading();
    }

    private void deleteFirstWithExpectation(char expected) {
        char found = json.getAndDeleteFirst();
        if(found != expected) throw new JsonParseException(expected, found);
    }

    private boolean startsWithThenRemove(String possibleStart) {
        if(json.startsWith(possibleStart)) {
            json.delete(possibleStart.length());
            return true;
        }
        return false;
    }



    private static boolean noEndOfLine(char c) {
        return c != ',' && c != ']' && c != '}' && c != '/'; // '/' -> Comment
    }
}
