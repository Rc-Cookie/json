package com.github.rccookie.json;

class JsonStringBuilder {

    private StringBuilder builder;



    public JsonStringBuilder(String string) {
        builder = new StringBuilder(string);
    }



    public JsonStringBuilder delete(int start, int end) {
        builder.delete(start, end);
        return this;
    }

    public int indexOf(String string) {
        return builder.indexOf(string);
    }



    @Override
    public String toString() {
        return builder.toString();
    }

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass", "EqualsOnSuspiciousObject"})
    @Override
    public boolean equals(Object o) {
        return builder.equals(o);
    }

    @Override
    public int hashCode() {
        return builder.hashCode();
    }



    public boolean isEmpty() {
        return builder.length() == 0;
    }

    public JsonStringBuilder stripLeading() {
        builder = new StringBuilder(toString().stripLeading());
        return this;
    }

    public JsonStringBuilder delete(int count) {
        return delete(0, count);
    }

    public JsonStringBuilder deleteFirst() {
        builder.deleteCharAt(0);
        return this;
    }

    public char getAndDeleteFirst() {
        char c = first();
        deleteFirst();
        return c;
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
}
