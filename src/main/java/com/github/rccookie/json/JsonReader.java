package com.github.rccookie.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

class JsonReader {

    private final BufferedReader reader;
    private int line = 1, charIndex = 1;



    public JsonReader(Reader reader) {
        this.reader = new BufferedReader(reader);
    }



    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int indexOf(char c) {
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

    public int indexOf(String string) {
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

    public String getPosition() {
        return "at " + line + ':' + charIndex;
    }



    public boolean isEmpty() {
        try {
            reader.mark(1);
            boolean empty = reader.read() == -1;
            reader.reset();
            return empty;
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public JsonReader skipWhitespaces() {
        try {
            reader.mark(1);
            int c;
            while(isWhitespace(c = reader.read())) {
                countRemoved((char)c);
                reader.mark(1);
            }
            reader.reset();
            return this;
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isWhitespace(int c) {
        if(c == -1) throw new JsonParseException("Reached end of file during parsing", this);
        return Character.isWhitespace(c);
    }

    public JsonReader skip(int count) {
        for(int i=0; i<count; i++) skip();
        return this;
    }

    public JsonReader skip() {
        read();
        return this;
    }

    public char read() {
        try {
            int c = reader.read();
            if(c == -1) throw new JsonParseException("Reached end of file during parsing", this);
            countRemoved((char)c);
            return (char)c;
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public JsonReader skipToContent() {
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

    public char peek() {
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

    public boolean startsWith(String string) {
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

    public boolean startsWith(char c) {
        return !isEmpty() && peek() == c;
    }


    private void countRemoved(char removed) {
        if(removed == '\n') {
            line++;
            charIndex = 1;
        }
        else charIndex++;
    }
}
