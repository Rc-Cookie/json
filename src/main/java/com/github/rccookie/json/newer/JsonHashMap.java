package com.github.rccookie.json.newer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import static com.github.rccookie.json.newer.JsonUtils.validateKey;
import static com.github.rccookie.json.newer.JsonUtils.validateValue;

class JsonHashMap extends HashMap<String,Object> {


    @Override
    public Object put(String key, Object value) {
        return super.put(validateKey(key), validateValue(value));
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        m.forEach(this::put);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entries = super.entrySet();
        Map<Entry<String, Object>, BackedEntry> mapping = new HashMap<>();
        for(var entry : entries)
            mapping.put(entry, new BackedEntry(entry));
        return new Set<>() {
            @Override
            public int size() {
                return entries.size();
            }

            @Override
            public boolean isEmpty() {
                return entries.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return entries.contains(o);
            }

            @NotNull
            @Override
            public Iterator<Entry<String, Object>> iterator() {
                Iterator<Entry<String, Object>> iterator = entries.iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Entry<String, Object> next() {
                        Entry<String, Object> next = iterator.next();
                        BackedEntry entry;
                        if(!mapping.containsKey(next))
                            mapping.put(next, entry = new BackedEntry(next));
                        else entry = mapping.get(next);
                        return entry;
                    }
                };
            }

            @NotNull
            @Override
            public Object[] toArray() {
                return entries.toArray();
            }

            @NotNull
            @Override
            public <T> T[] toArray(@NotNull T[] a) {
                //noinspection SuspiciousToArrayCall
                return entries.toArray(a);
            }

            @Override
            public boolean add(Entry<String, Object> stringObjectEntry) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsAll(@NotNull Collection<?> c) {
                return entries.containsAll(c);
            }

            @Override
            public boolean addAll(@NotNull Collection<? extends Entry<String, Object>> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(@NotNull Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(@NotNull Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return super.getOrDefault(key, validateValue(defaultValue));
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return super.putIfAbsent(key, validateValue(value));
    }

    @Override
    public boolean remove(Object key, Object value) {
        return super.remove(key, value);
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        return super.replace(key, oldValue, validateValue(newValue));
    }

    @Override
    public Object replace(String key, Object value) {
        return super.replace(key, validateValue(value));
    }

    @Override
    public Object computeIfAbsent(String key, Function<? super String, ?> mappingFunction) {
        return super.computeIfAbsent(key, k -> validateValue(mappingFunction.apply(k)));
    }

    @Override
    public Object computeIfPresent(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return super.computeIfPresent(key, (k,v) -> validateValue(remappingFunction.apply(k, v)));
    }

    @Override
    public Object compute(String key, BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return super.compute(key, (k,v) -> validateValue(remappingFunction.apply(k, v)));
    }

    @Override
    public Object merge(String key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        return super.merge(key, validateValue(value), (k,v) -> validateValue(remappingFunction.apply(k, v)));
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        super.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
        super.replaceAll((k,v) -> validateValue(function.apply(k, v)));
    }

    @Override
    public Object clone() {
        JsonHashMap clone = new JsonHashMap();
        clone.putAll(this);
        return clone;
    }



    private static class BackedEntry implements Entry<String, Object> {

        private final Entry<String, Object> entry;

        public BackedEntry(Entry<String, Object> entry) {
            this.entry = entry;
        }

        @Override
        public String getKey() {
            return entry.getKey();
        }

        @Override
        public Object getValue() {
            return entry.getValue();
        }

        @Override
        public Object setValue(Object value) {
            return entry.setValue(validateValue(value));
        }
    }
}
