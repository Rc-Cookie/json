package com.github.rccookie.json;

class JsonStringBuilder {

    private StringBuilder builder;
    private int line = 1, charIndex = 1;



    public JsonStringBuilder(String string) {
        builder = new StringBuilder(string);
    }



    public int indexOf(String string) {
        return builder.indexOf(string);
    }



    @Override
    public String toString() {
        return builder.toString();
    }

    public String getPosition() {
        return "at " + line + ':' + charIndex;
    }



    public boolean isEmpty() {
        return builder.length() == 0;
    }

    public JsonStringBuilder stripLeading() {
        String striped = toString().stripLeading();
        countRemoved(builder.substring(0, builder.length() - striped.length()));
        builder = new StringBuilder(striped);
        return this;
    }

    public JsonStringBuilder delete(int count) {
        countRemoved(builder.substring(0, count));
        builder.delete(0, count);
        return this;
    }

    public JsonStringBuilder deleteFirst() {
        popFirst();
        return this;
    }

    public char popFirst() {
        char c = first();
        builder.deleteCharAt(0);
        countRemoved(c);
        return c;
    }

    public JsonStringBuilder stripToContent() {
        if(stripLeading().startsWith("//")) {
            int index = indexOf('\n');
            if(index == -1) throw new JsonParseException("Reached end of file during comment", this);
            delete(index + 1);
            return stripToContent();
        }
        if(startsWith("/*")) {
            int index = indexOf("*/");
            if(index == -1) throw new JsonParseException("Reached end of file during comment", this);
            delete(index + 2);
            return stripToContent();
        }
        return stripLeading();
    }

    public char first() {
        return builder.charAt(0);
    }

    public boolean startsWith(String string) {
        return builder.toString().startsWith(string);
    }

    public boolean startsWith(char c) {
        return !isEmpty() && builder.charAt(0) == c;
    }

    public int indexOf(char c) {
        return indexOf(c + "");
    }


    private void countRemoved(String removed) {
        for(char c : removed.toCharArray())
            countRemoved(c);
    }

    private void countRemoved(char removed) {
        if(removed == '\n') {
            line++;
            charIndex = 1;
        }
        else charIndex++;
    }
}
