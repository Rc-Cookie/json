package de.rccookie.json.linked;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import de.rccookie.json.JsonArray;
import de.rccookie.json.JsonDeserializer;
import de.rccookie.json.JsonElement;
import de.rccookie.json.JsonObject;
import de.rccookie.json.JsonStructure;

/**
 * A json deserializer that, in combination with {@link LinkedSerializer}, retains
 * relationships between objects where multiple instances share a reference to the
 * same instance of an object, in other words, it maintains the <code>==</code>
 * relationship between serialized objects. Note that cyclic references between
 * objects are still not supported and may lead to {@link StackOverflowError}s.
 */
public class LinkedDeserializer extends JsonDeserializer {

    public LinkedDeserializer() {
        super(true);
    }

    public JsonElement wrap(JsonElement json) {
        return wrap(json, json.withDeserializer(DEFAULT).get());
    }

    private JsonElement wrap(JsonElement context, Object value) {
        return JsonElement.wrap(new JsonArray(assertType(context, value, JsonArray.class).size() - 1), new Instance(context)).withContextOf(context);
    }

    @Override
    protected Object deserialize(Type type, JsonElement context, Object value) {
        return wrap(context, value).as(type);
    }

    @Override
    protected Object get(JsonElement context, Object value) {
        return wrap(context, value).get();
    }

    @Override
    protected JsonStructure asStructure(JsonElement context, Object value) {
        return value != null ? wrap(context, value).asStructure() : null;
    }

    @Override
    protected boolean isStructure(JsonElement context, Object value) {
        return value == null || wrap(context, value).isStructure();
    }

    @Override
    protected JsonObject asObject(JsonElement context, Object value) {
        return value != null ? wrap(context, value).asObject() : null;
    }

    @Override
    protected boolean isObject(JsonElement context, Object value) {
        return value == null || wrap(context, value).isObject();
    }

    @Override
    protected JsonArray asArray(JsonElement context, Object value) {
        return value != null ? wrap(context, value).asArray() : null;
    }

    @Override
    protected boolean isArray(JsonElement context, Object value) {
        return value == null || wrap(context, value).isArray();
    }

    @Override
    protected Iterable<JsonElement> elements(JsonElement context, Object value) {
        return wrap(context, value).nullableElements();
    }

    @Override
    protected void forEach(JsonElement context, Object value, BiConsumer<? super String, ? super JsonElement> action) {
        wrap(context, value).forEachNullable(action);
    }

    @Override
    protected Set<String> keySet(JsonElement context, Object value) {
        return wrap(context, value).keySet();
    }

    @Override
    protected Collection<JsonElement> values(JsonElement context, Object value) {
        return wrap(context, value).values();
    }

    @Override
    protected Stream<JsonElement> stream(JsonElement context, Object value) {
        return wrap(context, value).stream();
    }

    @Override
    protected int size(JsonElement context, Object value) {
        return wrap(context, value).size();
    }

    @Override
    protected JsonElement get(JsonElement context, Object value, String key) {
        return wrap(context, value).get(key);
    }

    @Override
    protected JsonElement get(JsonElement context, Object value, int index) {
        return wrap(context, value).get(index);
    }

    private static final class Instance extends JsonDeserializer {

        private final JsonElement json;
        private final Object[] deserialized;
        private final Set<JsonArray> valueArrays = Collections.newSetFromMap(new IdentityHashMap<>());

        Instance(JsonElement json) {
            super(false);
            this.json = Objects.requireNonNull(json, "json").withDeserializer(DEFAULT);
            JsonArray arr = this.json.asArray();
            deserialized = new Object[arr.size()];
            for(Object o : arr)
                if(o instanceof JsonArray)
                    valueArrays.add((JsonArray) o);
        }

        @Override
        protected Object deserialize(Type type, JsonElement data, Object value) {
            if(!(value instanceof JsonArray) || valueArrays.contains(value))
                return super.deserialize(type, data, value);

            int id = assertType(data, value, JsonArray.class).getInt(0);

            if(deserialized[id] == null)
                deserialized[id] = super.deserialize(type, json.get(id).withDeserializer(this).withContextOf(data), json.get(id).get());
            assert deserialized[id] != null;

            return deserialized[id];
        }

        private JsonElement followRef(JsonElement context, Object ref) {
            return json.get(((JsonArray) ref).getInt(0)).withDeserializer(this).withContextOf(context);
//            return JsonElement.wrap(ref, this).withContextOf(context);
        }

        @Override
        protected Object get(JsonElement context, Object value) {
            if(!(value instanceof JsonArray))
                return value;
            return asStructure(context, value);
        }

        @Override
        protected JsonStructure asStructure(JsonElement context, Object value) {
            if(!(value instanceof JsonArray) || valueArrays.contains(value)) {
                JsonStructure val = super.asStructure(context, value);
                if(val instanceof JsonArray) {
                    JsonArray arr = new JsonArray();
                    context.forEachNullable(o -> arr.add(get(JsonElement.wrap(o, this), o)));
                    return arr;
                }
                else {
                    JsonObject obj = new JsonObject();
                    ((JsonObject) val).forEach((k,v) -> obj.put(k, get(JsonElement.wrap(v, this), v)));
                    return obj;
                }
            }

            return followRef(context, value).as(JsonStructure.class);
        }

        @Override
        protected boolean isStructure(JsonElement context, Object value) {
            if(!(value instanceof JsonArray) || valueArrays.contains(value))
                return super.isStructure(context, value);

            int id = assertType(context, value, JsonArray.class).getInt(0);
            return json.get(id).isStructure();
        }

        @Override
        protected JsonObject asObject(JsonElement context, Object value) {
            if(!(value instanceof JsonArray) || valueArrays.contains(value)) {
                JsonObject obj = new JsonObject();
                super.asObject(context, value).forEach((k,v) -> obj.put(k, get(JsonElement.wrap(v, this), v)));
                return obj;
            }

            return followRef(context, value).as(JsonObject.class);
        }

        @Override
        protected boolean isObject(JsonElement context, Object value) {
            if(!(value instanceof JsonArray) || valueArrays.contains(value))
                return super.isObject(context, value);

            int id = assertType(context, value, JsonArray.class).getInt(0);
            return json.get(id).isObject();
        }

        @Override
        protected JsonArray asArray(JsonElement context, Object value) {
            if(!(value instanceof JsonArray) || valueArrays.contains(value)) {
                JsonArray obj = new JsonArray();
                super.asArray(context, value).forEach(o -> obj.add(get(JsonElement.wrap(o, this), o)));
                return obj;
            }

            return followRef(context, value).as(JsonArray.class);
        }

        @Override
        protected boolean isArray(JsonElement context, Object value) {
            if(!(value instanceof JsonArray) || valueArrays.contains(value))
                return super.isArray(context, value);

            int id = assertType(context, value, JsonArray.class).getInt(0);
            return json.get(id).isArray();
        }

        @Override
        protected JsonElement get(JsonElement context, Object value, String key) {
            if(value instanceof JsonArray && !valueArrays.contains(value)) {
                context = followRef(context, value);
                value = context.withDeserializer(DEFAULT).get();
            }
            return JsonElement.wrap(assertType(context, value, JsonObject.class).get(key), this).withContextOf(context);
        }

        @Override
        protected JsonElement get(JsonElement context, Object value, int index) {
            if(value instanceof JsonArray && !valueArrays.contains(value)) {
                context = followRef(context, value);
                value = context.withDeserializer(DEFAULT).get();
            }
            return JsonElement.wrap(assertType(context, value, JsonArray.class).get(index), this).withContextOf(context);
        }

        @Override
        protected Iterable<JsonElement> elements(JsonElement context, Object value) {
            return () -> stream(context, value).iterator();
        }

        @Override
        protected void forEach(JsonElement context, Object value, BiConsumer<? super String, ? super JsonElement> action) {
            for(String k : keySet(context, value))
                action.accept(k, context.get(k));
        }

        @Override
        protected Set<String> keySet(JsonElement context, Object value) {
            if(value instanceof JsonArray && !valueArrays.contains(value)) {
                context = followRef(context, value);
                value = context.withDeserializer(DEFAULT).get();
            }
            return assertType(context, value, JsonObject.class).keySet();
        }

        @Override
        protected Collection<JsonElement> values(JsonElement context, Object value) {
            Collection<JsonElement> values = new ArrayList<>();
            for(String k : keySet(context, value))
                values.add(context.get(k));
            return values;
        }

        @Override
        protected Stream<JsonElement> stream(JsonElement context, Object value) {
            if(value instanceof JsonArray && !valueArrays.contains(value)) {
                context = followRef(context, value);
                value = context.withDeserializer(DEFAULT).get();
            }
            JsonArray arr = assertType(context, value, JsonArray.class);
            return IntStream.range(0, arr.size()).mapToObj(context::get);
        }

        @Override
        protected int size(JsonElement context, Object value) {
            if(value instanceof JsonArray && !valueArrays.contains(value)) {
                context = followRef(context, value);
                value = context.withDeserializer(DEFAULT).get();
            }
            JsonStructure obj = assertType(context, value, JsonStructure.class);
            return obj.size();
        }
    }
}
