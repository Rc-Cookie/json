package com.github.rccookie.json.newer;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

import com.github.rccookie.json.JsonSerializable;

import org.jetbrains.annotations.NotNull;

public class JsonObject implements Iterable<JsonValue>, Cloneable, JsonSerializable {

    private final Map<String, Object> map = new JsonHashMap();


    @Override
    public Object toJson() {
        return this;
    }

    @NotNull
    @Override
    public Iterator<JsonValue> iterator() {
        return map.values().stream().map(JsonValue::of).iterator();
    }

    @Override
    public JsonObject clone() {
        return new JsonObject(this);
    }



    public void set(String path, Object value) {
        String key;

        int bracketIndex = path.indexOf('[');
        int dotIndex = path.indexOf('.');
        boolean nextIsObj = true;
        String nextPath = null;
        if(bracketIndex == -1 && dotIndex == -1)
            key = path;
        else {
            if(bracketIndex != -1 && (dotIndex == -1 || bracketIndex < dotIndex)) {
                key = path.substring(0, bracketIndex);
                nextPath = path.substring(bracketIndex);
                nextIsObj = false;
            }
            else {
                key = path.substring(0, dotIndex);
                nextPath = path.substring(dotIndex + 1);
            }
        }

        if(nextPath != null) {
            if(nextIsObj) {
                JsonObject current = (JsonObject) map.get(key);
                if(current == null)
                    map.put(key, current = new JsonObject());
                current.set(nextPath, value);
            }
            else {
                JsonArray current = (JsonArray) map.get(key);
                if(current == null)
                    map.put(key, current = new JsonArray());
                current.set(nextPath, value);
            }
        }
        else setDirect(key, value);
    }

    public void setDirect(String key, Object value) {
        map.put(key, value);
    }



    public Object getDirect(String key) {
        return map.get(key);
    }

    public JsonValue getValueDirect(String key) {
        return JsonValue.of(getDirect(key));
    }

    public JsonValue getValue(String path) {
        String key;

        int bracketIndex = path.indexOf('[');
        int dotIndex = path.indexOf('.');
        boolean nextIsObj = true;
        String nextPath = null;
        if(bracketIndex == -1 && dotIndex == -1)
            key = path;
        else {
            if(bracketIndex != -1 && (dotIndex == -1 || bracketIndex < dotIndex)) {
                key = path.substring(0, bracketIndex);
                nextPath = path.substring(bracketIndex);
                nextIsObj = false;
            }
            else {
                key = path.substring(0, dotIndex);
                nextPath = path.substring(dotIndex + 1);
            }
        }

        if(nextPath != null) {
            if(nextIsObj) {
                JsonObject current = (JsonObject) map.get(key);
                if(current == null)
                    map.put(key, current = new JsonObject());
                return current.getValue(nextPath);
            }
            else {
                JsonArray current = (JsonArray) map.get(key);
                if(current == null)
                    map.put(key, current = new JsonArray());
                return current.getValue(nextPath);
            }
        }
        else return getValueDirect(key);
    }

    public Object get(String path) {
        String key;

        int bracketIndex = path.indexOf('[');
        int dotIndex = path.indexOf('.');
        boolean nextIsObj = true;
        String nextPath = null;
        if(bracketIndex == -1 && dotIndex == -1)
            key = path;
        else {
            if(bracketIndex != -1 && (dotIndex == -1 || bracketIndex < dotIndex)) {
                key = path.substring(0, bracketIndex);
                nextPath = path.substring(bracketIndex);
                nextIsObj = false;
            }
            else {
                key = path.substring(0, dotIndex);
                nextPath = path.substring(dotIndex + 1);
            }
        }

        if(nextPath != null) {
            if(nextIsObj) {
                JsonObject current = (JsonObject) map.get(key);
                if(current == null) return null;
                return current.get(nextPath);
            }
            else {
                JsonArray current = (JsonArray) map.get(key);
                if(current == null) return null;
                return current.get(nextPath);
            }
        }
        else return getDirect(key);
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


    public JsonObject getObjectDirect(String key) {
        return (JsonObject) getDirect(key);
    }

    public JsonArray getArrayDirect(String key) {
        return (JsonArray) getDirect(key);
    }

    public String getStringDirect(String key) {
        return (String) getDirect(key);
    }

    public Number getNumberDirect(String key) {
        return (Number) getDirect(key);
    }

    public long getLongDirect(String key) {
        return getNumberDirect(key).longValue();
    }

    public int getIntDirect(String key) {
        return getNumberDirect(key).intValue();
    }

    public double getDoubleDirect(String key) {
        return getNumberDirect(key).doubleValue();
    }

    public float getFloatDirect(String key) {
        return getNumberDirect(key).floatValue();
    }

    public Boolean getBoolDirect(String key) {
        return (Boolean) getDirect(key);
    }



    public boolean containsKey(String key) {
        return map
    }



    public void forEach(BiConsumer<String, JsonValue> action) {
        for(Map.Entry<String, Object> entry : map.entrySet())
            action.accept(entry.getKey(), JsonValue.of(entry.getValue()));
    }

    public void merge(JsonObject other) {
        other.forEach((k,v) -> {

            if(v == null) putIfAbsent(k, null);
            else if(v instanceof com.github.rccookie.json.JsonObject) {
                com.github.rccookie.json.JsonObject current = getElement(k).orNull();
                if(current == null) put(k, v);
                else current.combine((com.github.rccookie.json.JsonObject) v);
            }
            else if(v instanceof com.github.rccookie.json.JsonArray) {
                com.github.rccookie.json.JsonArray current = getElement(k).orNull();
                if(current == null) put(k, v);
                else current.combine((com.github.rccookie.json.JsonArray) v);
            }
            else if(!containsKey(k)) put(k, v);
        });
    }



    public Map<String, Object> map() {
        return map;
    }
}
