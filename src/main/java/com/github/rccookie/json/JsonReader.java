package com.github.rccookie.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

/**
 * Internal class to read from a {@link Reader} more conveniently.
 */
@SuppressWarnings({"UnusedReturnValue", "BooleanMethodIsAlwaysInverted", "SameParameterValue"})
class JsonReader implements AutoCloseable {

    /**
     * The underlying reader.
     */
    private final Reader reader;
    /**
     * Current position in the string.
     */
    private int line = 1, charIndex = 1;


    /**
     * Creates a new json reader using the given reader.
     *
     * @param reader The reader to use
     */
    JsonReader(Reader reader) {
        this.reader = reader.markSupported() ? reader : new BufferedReader(reader);
    }



    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the first index of the given char, or {@code -1} if this reader
     * does not contain the specified character.
     *
     * @param c The character to search for
     * @return First index of the character or {@code -1}
     */
    int indexOf(char c) {
        try {
            for(int i=0;;i++) {
                reader.mark(i+1);
                //noinspection ResultOfMethodCallIgnored
                reader.skip(i);
                int d = reader.read();
                reader.reset();
                if(d == c) return i;
                if(d == -1) return -1;
            }
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the first index of the given string, or {@code -1} if this reader
     * does not contain the specified character.
     *
     * @param string The string to search for
     * @return First index of the string or {@code -1}
     */
    int indexOf(String string) {
        if(string.isEmpty()) return 0;
        try {
            iLoop: for(int i=0;;i++) {
                reader.mark(i+1);
                //noinspection ResultOfMethodCallIgnored
                reader.skip(i);
                int d = reader.read();
                if(d == string.charAt(0)) {
                    for(int j=1; j<string.length(); j++) {
                        if(string.charAt(j) != reader.read()) {
                            reader.reset();
                            continue iLoop;
                        }
                    }
                    reader.reset();
                    return i;
                }
                reader.reset();
                if(d == -1) return -1;
            }
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }



    @Override
    public String toString() {
        return "JsonReader{" + reader + "}";
    }

    /**
     * Returns a string with the current position in the reader, in the form
     * {@code line:char}.
     *
     * @return The current position in the reader
     */
    String getPosition() {
        return line + ":" + charIndex;
    }


    /**
     * Returns {@code true} if the underlying reader has no more characters.
     *
     * @return If no more characters are available
     */
    boolean isEmpty() {
        try {
            reader.mark(1);
            boolean empty = reader.read() == -1;
            reader.reset();
            return empty;
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Skips all whitespaces at the start of the string as specified in
     * {@link Character#isWhitespace(char)}.
     *
     * @return This json reader
     */
    JsonReader skipWhitespaces() {
        try {
            reader.mark(1);
            int c;
            while(Character.isWhitespace(c = reader.read())) {
                countRead((char)c);
                reader.mark(1);
            }
            reader.reset();
            return this;
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Skips the specified number of characters.
     *
     * @param count The number of characters to be skipped
     * @return This json reader
     * @throws JsonParseException If the end of the reader is reached
     */
    JsonReader skip(int count) {
        for(int i=0; i<count; i++) skip();
        return this;
    }

    /**
     * Skips the next character.
     *
     * @return This json reader
     * @throws JsonParseException If the end of the reader is reached
     */
    JsonReader skip() {
        read();
        return this;
    }

    /**
     * Skips the first character if it is the specified character.
     *
     * @param start The character to test as starting character
     * @return Whether the string started with the specified char
     */
    boolean skipIf(char start) {
        if(startsWith(start)) {
            skip();
            return true;
        }
        return false;
    }

    /**
     * Skips the length of the specified string if this string starts
     * with that string.
     *
     * @param start The string to test as starting string
     * @return Whether the string started with the specified string
     */
    boolean skipIf(String start) {
        if(startsWith(start)) {
            skip(start.length());
            return true;
        }
        return false;
    }

    /**
     * Skips the first char and throws a {@link JsonParseException} if it
     * does not match the expected character.
     *
     * @param expected The expected first char
     * @return This json reader
     * @throws JsonParseException If the first character was not the expected
     *                            one or there are no more characters
     */
    JsonReader skipExpected(char expected) {
        char found = read();
        if(found != expected) throw new JsonParseException(expected, found, this);
        return this;
    }

    /**
     * Reads and removes the next character.
     *
     * @return The next character
     * @throws JsonParseException If the end of the reader is reached
     */
    char read() {
        try {
            int c = reader.read();
            if(c == -1) throw new JsonParseException("Reached end of file during parsing", this);
            countRead((char)c);
            return (char)c;
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads and removes the specified number of characters.
     *
     * @param count The number of characters to read
     * @return The next characters
     * @throws JsonParseException If the end of the reader is reached
     */
    String read(int count) {
        StringBuilder string = new StringBuilder(count);
        for(int i=0; i<count; i++) string.append(read());
        return string.toString();
    }

    /**
     * Skips whitespaces, comments and newlines.
     *
     * @return This json reader
     * @throws JsonParseException If the end of the reader is reached
     */
    JsonReader skipToContent() {
        if(skipWhitespaces().startsWith("//")) {
            int index = indexOf('\n');
            if(index == -1) throw new JsonParseException("Reached end of file during comment", this);
            skip(index + 1);
            return skipToContent();
        }
        if(startsWith("/*")) {
            int index = indexOf("*/");
            if(index == -1) throw new JsonParseException("Reached end of file during comment", this);
            skip(index + 2);
            return skipToContent();
        }
        return skipWhitespaces();
    }

    /**
     * Returns the next character without removing it.
     *
     * @return The next character
     * @throws JsonParseException If the end of the reader is reached
     */
    char peek() {
        try {
            reader.mark(1);
            int c = reader.read();
            reader.reset();
            if(c == -1) throw new JsonParseException("Reached end of file during parsing", this);
            return (char)c;
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the next character replacing control characters with appropriate
     * descriptions.
     *
     * @return The next char description
     */
    String peekDescription() {
        char c = peek();
        switch(c) {
            case '\n':
            case '\r': return "<end of line>";
            case '\t':
            case '\f': return " ";
            case '\b': return "<backspace>";
            default: return c+"";
        }
    }

    /**
     * Determines whether the reader starts with the given string.
     *
     * @param string The string to test for starting
     * @return Whether the reader starts with the given string
     */
    boolean startsWith(String string) {
        try {
            reader.mark(string.length());
            for(int i=0; i<string.length(); i++) {
                if(reader.read() != string.charAt(i)) {
                    reader.reset();
                    return false;
                }
            }
            reader.reset();
            return true;
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Determines whether the reader starts with the given character
     *
     * @param c The char to test for starting
     * @return Whether the reader starts with the given char
     */
    boolean startsWith(char c) {
        return !isEmpty() && peek() == c;
    }

    /**
     * Determines whether the next character ends a value.
     *
     * @return Whether the next character is a value end character
     * @throws JsonParseException If the end of the reader is reached
     */
    boolean endOfValue() {
        char c = peek();
        return c == '\n' || c == ' ' || c == ',' || c == ']' || c == '}' || c == '/'; // '/' -> Comment
    }


    /**
     * Counts up the position counters according to the read character
     *
     * @param read The character that was read
     */
    private void countRead(char read) {
        if(read == '\n') {
            line++;
            charIndex = 1;
        }
        else charIndex++;
    }
}
