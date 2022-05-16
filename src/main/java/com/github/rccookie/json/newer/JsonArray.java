package com.github.rccookie.json.newer;

import java.util.Iterator;
import java.util.List;

import com.github.rccookie.json.JsonSerializable;

import org.jetbrains.annotations.NotNull;

public class JsonArray implements Iterable<JsonValue>, Cloneable, JsonSerializable {

    private final List<Object> list = new JsonArrayList();



    @Override
    public Object toJson() {
        return this;
    }

    @NotNull
    @Override
    public Iterator<JsonValue> iterator() {
        return list.stream().map(JsonValue::of).iterator();
    }

    @Override
    public JsonArray clone() {
        return new JsonArray(this);
    }



    public void set(String path, Object value) {
        int index;
        if(!path.startsWith("["))
            throw new JsonPathSyntaxException("'[' expected");
        int closeIndex = path.indexOf(']');
        if(closeIndex < 0)
            throw new JsonPathSyntaxException("No closing bracket found");
        try { index = Integer.parseInt(path.substring(1, closeIndex)); }
        catch(NumberFormatException e) { throw new JsonPathSyntaxException(e); }

        String nextPath = path.substring(closeIndex + 1);
        boolean nextIsObj = true;
        if(nextPath.isEmpty()) nextPath = null;
        else {
            if(nextPath.startsWith("["))
                nextIsObj = false;
            else if(!nextPath.startsWith("."))
                throw new JsonPathSyntaxException("'[' or '.' expected");
        }

        if(nextPath != null) {
            if(nextIsObj) {
                JsonObject current;
                while(index < list.size() - 1) list.add(null);
                current = (JsonObject) list.get(index);
                if(current == null)
                    list.set(index, current = new JsonObject());
                current.set(nextPath, value);
            }
            else {
                JsonArray current;
                while(index < list.size() - 1) list.add(null);
                current = (JsonArray) list.get(index);
                if(current == null)
                    list.set(index, current = new JsonArray());
                current.set(nextPath, value);
            }
        }
        else set(index, value);
    }

    public void set(int index, Object value) {
        list.set(index, value);
    }

    public void add(Object value) {
        list.add(value);
    }



    public Object get(int index) {
        return list.get(index);
    }

    public JsonValue getValue(int index) {
        return JsonValue.of(get(index));
    }

    public JsonValue getValue(String path) {
        return JsonValue.of(get(path));
    }

    public Object get(String path) {
        int index;
        if(!path.startsWith("["))
            throw new JsonPathSyntaxException("'[' expected");
        int closeIndex = path.indexOf(']');
        if(closeIndex < 0)
            throw new JsonPathSyntaxException("No closing bracket found");
        try { index = Integer.parseInt(path.substring(1, closeIndex)); }
        catch(NumberFormatException e) { throw new JsonPathSyntaxException(e); }

        String nextPath = path.substring(closeIndex + 1);
        boolean nextIsObj = true;
        if(nextPath.isEmpty()) nextPath = null;
        else {
            if(nextPath.startsWith("["))
                nextIsObj = false;
            else if(!nextPath.startsWith("."))
                throw new JsonPathSyntaxException("'[' or '.' expected");
        }

        if(nextPath != null) {
            if(nextIsObj) {
                JsonObject current;
                while(index < list.size() - 1) list.add(null);
                current = (JsonObject) list.get(index);
                if(current == null)
                    list.set(index, current = new JsonObject());
                return current.get(nextPath);
            }
            else {
                JsonArray current;
                while(index < list.size() - 1) list.add(null);
                current = (JsonArray) list.get(index);
                if(current == null)
                    list.set(index, current = new JsonArray());
                return current.get(nextPath);
            }
        }
        else return get(index);
    }

    public JsonObject getObject(String path) {
        return (JsonObject) get(path);
    }

    public JsonArray getArray(String path) {
        return (JsonArray) get(path);
    }

    public String getString(String path) {
        return (String) get(path);
    }

    public Number getNumber(String path) {
        return (Number) get(path);
    }

    public long getLong(String path) {
        return getNumber(path).longValue();
    }

    public int getInt(String path) {
        return getNumber(path).intValue();
    }

    public double getDouble(String path) {
        return getNumber(path).doubleValue();
    }

    public float getFloat(String path) {
        return getNumber(path).floatValue();
    }

    public Boolean getBool(String path) {
        return (Boolean) get(path);
    }


    public JsonObject getObject(int index) {
        return (JsonObject) get(index);
    }

    public JsonArray getArray(int index) {
        return (JsonArray) get(index);
    }

    public String getString(int index) {
        return (String) get(index);
    }

    public Number getNumber(int index) {
        return (Number) get(index);
    }

    public long getLong(int index) {
        return getNumber(index).longValue();
    }

    public int getInt(int index) {
        return getNumber(index).intValue();
    }

    public double getDouble(int index) {
        return getNumber(index).doubleValue();
    }

    public float getFloat(int index) {
        return getNumber(index).floatValue();
    }

    public Boolean getBool(int index) {
        return (Boolean) get(index);
    }



    public List<Object> list() {
        return list;
    }
}
