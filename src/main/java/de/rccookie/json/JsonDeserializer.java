package de.rccookie.json;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import de.rccookie.util.IterableIterator;

/**
 * This class controls the "low level" deserialization and thus manages the level of allowed type conversion.
 */
public abstract class JsonDeserializer {

    /**
     * A json deserializer which strictly enforces correct types.
     */
    public static final JsonDeserializer STRICT = new JsonDeserializer(true) { };
    /**
     * A json deserializer which allows numbers and boolean values to be enquoted as strings, and the other way around.
     */
    public static final JsonDeserializer STRING_CONVERSION = new JsonDeserializer(false) { };
    /**
     * The json serializer used by default by the {@link JsonElement} class.
     */
    public static final JsonDeserializer DEFAULT = STRING_CONVERSION;
    /**
     * A json deserializer which attempts to convert between types as good as possible:
     * <ul>
     *     <li>Everything that is not a string will be converted to its json representation string</li>
     *     <li>Numbers can be converted with loss (i.e. floats to ints) and will be parsed from strings, true and false will be converted to 1 and 0</li>
     *     <li>"true" in any case is true, other strings are false. Numbers are true iff not 0. Objects or arrays are true, null is false.</li>
     *     <li>If an array (or a structure) is expected but not found, an array with a single item will be returned</li>
     * </ul>
     */
    public static final JsonDeserializer BEST_EFFORT = new BestEffortDeserializer();

    /**
     * Whether this json deserializer enforces correct types, or attempts some level of conversion.
     */
    protected final boolean strictTypes;

    protected JsonDeserializer(boolean strictTypes) {
        this.strictTypes = strictTypes;
    }


    protected Object get(JsonElement context, Object value) {
        return value;
    }

    /**
     * Deserializes the given json data into the given type using
     * the mapped deserializer or the constructor of the type with
     * one argument of type {@link JsonElement}.
     *
     * @param type  The type to deserialize to
     * @param data  The data to deserialize
     * @param value THe value of the json object
     * @return The deserialized value
     * @throws IllegalStateException            If no deserializer is registered for
     *                                          the specified type
     * @throws IllegalJsonDeserializerException If an exception occurs while loading the
     *                                          deserializer of the class to deserialize to
     */
    protected Object deserialize(Type type, JsonElement data, Object value) {
        return JsonSerialization.deserialize(type, data);
    }

    protected <T> T assertType(JsonElement context, Object value, Class<T> type) throws TypeMismatchException {
        try {
            return Json.cast(value, type, context);
        } catch(ClassCastException|NullPointerException e) {
            throw new TypeMismatchException(context.path(), type, value != null ? value.getClass() : null);
        }
    }

    /**
     * Returns the value as {@link JsonStructure}.
     *
     * @return The value as json structure
     * @throws TypeMismatchException If a value is present and is not a json structure
     */
    protected JsonStructure asStructure(JsonElement context, Object value) {
        return assertType(context, value, JsonStructure.class);
    }

    /**
     * Returns {@code true} if the value is null or convertible to a {@link JsonStructure}.
     *
     * @return Whether the value is a json structure
     */
    protected boolean isStructure(JsonElement context, Object value) {
        return value == null || value instanceof JsonStructure;
    }

    /**
     * Returns the value as {@link JsonObject}.
     *
     * @return The value as json object
     * @throws TypeMismatchException If a value is present and is not a json object
     */
    protected JsonObject asObject(JsonElement context, Object value) {
        return assertType(context, value, JsonObject.class);
    }

    /**
     * Returns {@code true} if the value is null or convertible to a {@link JsonObject}.
     *
     * @return Whether the value is a json object
     */
    protected boolean isObject(JsonElement context, Object value) {
        return value == null || value instanceof JsonObject;
    }

    /**
     * Returns the value as {@link JsonArray}.
     *
     * @return The value as json array
     * @throws TypeMismatchException If al value is present and is not a json array
     */
    protected JsonArray asArray(JsonElement context, Object value) {
        return assertType(context, value, JsonArray.class);
    }

    /**
     * Returns {@code true} if the value is null or convertible to a {@link JsonArray}.
     *
     * @return Whether the value is a json array
     */
    protected boolean isArray(JsonElement context, Object value) {
        return value == null || value instanceof JsonArray;
    }

    /**
     * Returns the value as {@link String}.
     *
     * @return The value as string
     * @throws TypeMismatchException If a value is present and is not a string
     */
    protected String asString(JsonElement context, Object value) {
        if(!strictTypes && (value instanceof Number || value instanceof Boolean))
            return ""+value;
        return assertType(context, value, String.class);
    }

    /**
     * Returns {@code true} if the value is null or convertible to a {@link String}.
     *
     * @return Whether the value is a string
     */
    protected boolean isString(JsonElement context, Object value) {
        return value == null || value instanceof String || (!strictTypes && (value instanceof Number || value instanceof Boolean));
    }

    /**
     * Returns the value as a number.
     *
     * @return The value as number
     * @throws TypeMismatchException If a value is present and is not a number
     */
    protected Number asNumber(JsonElement context, Object value) {
        if(!strictTypes && value instanceof String) try {
            if(((String) value).isEmpty())
                return null;
            return Double.parseDouble((String) value);
        } catch(NumberFormatException ignored) { }
        return assertType(context, value, Number.class);
    }

    /**
     * Returns {@code true} if the value is null or convertible to a {@code Number}.
     *
     * @return Whether the value is a number
     */
    protected boolean isNumber(JsonElement context, Object value) {
        //noinspection ResultOfMethodCallIgnored
        return value == null || value instanceof Number || stringNumberTest(value, Double::parseDouble);
    }

    private boolean stringNumberTest(Object value, Consumer<? super String> parser) {
        if(strictTypes || !(value instanceof String))
            return false;
        String str = (String) value;
        if(str.isEmpty())
            return true;
        try {
            parser.accept(str);
            return true;
        } catch(NumberFormatException ignored) { }
        return false;
    }

    /**
     * Returns the value as {@code long}.
     *
     * @return The value as long
     * @throws TypeMismatchException If a value is present and is not a number that can be converted to a long without data loss
     */
    protected Long asLong(JsonElement context, Object value) {
        if(value == null) return null;

        if(!strictTypes && value instanceof String) try {
            if(((String) value).isEmpty())
                return null;
            return Long.parseLong((String) value);
        } catch(NumberFormatException ignored) { }

        Number n = assertType(context, value, Number.class);
        if(value instanceof Long || value instanceof Integer || n.doubleValue() == n.longValue())
            return n.longValue();
        throw new TypeMismatchException(context.path(), "Lossy conversion from "+n.getClass().getSimpleName()+" to Long");
    }

    /**
     * Returns {@code true} if the value is null, convertible to an {@link Integer},
     * {@link Long}, {@link Short} or {@link Byte}, or convertible to a {@link Double} or {@link Float}
     * which can be converted loss-less to a <code>long</code>.
     *
     * @return Whether the value is an integer number
     */
    protected boolean isInteger(JsonElement context, Object value) {
        if(value == null || value instanceof Long || value instanceof Integer
           || value instanceof Byte || value instanceof Short) return true;
        if(value instanceof Double || value instanceof Float)
            return ((Number) value).doubleValue() == ((Number) value).longValue();
        return stringNumberTest(value, Long::parseLong);
    }

    /**
     * Returns the value as {@code int}.
     *
     * @return The value as int
     * @throws TypeMismatchException If a value is present and
     *                            is not a number
     */
    protected Integer asInt(JsonElement context, Object value) {
        if(value == null) return null;

        if(!strictTypes && value instanceof String) try {
            if(((String) value).isEmpty())
                return null;
            return Integer.parseInt((String) value);
        } catch(NumberFormatException ignored) { }

        Number n = assertType(context, value, Number.class);
        if(value instanceof Integer || (value instanceof Long && n.longValue() == n.intValue())
           || ((value instanceof Double || value instanceof Float) && n.doubleValue() == n.intValue()))
            return n.intValue();
        throw new TypeMismatchException(context.path(), "Lossy conversion from "+n.getClass().getSimpleName()+" to Integer");
    }

    /**
     * Returns the value as {@code double}.
     *
     * @return The value as double
     * @throws TypeMismatchException If a value is present and
     *                            is not a number
     */
    protected Double asDouble(JsonElement context, Object value) {
        return context.get() != null ? asNumber(context, value).doubleValue() : null;
    }

    /**
     * Returns the value as {@code float}.
     *
     * @return The value as float
     * @throws TypeMismatchException If a value is present and
     *                            is not a number
     */
    protected Float asFloat(JsonElement context, Object value) {
        if(value == null) return null;

        if(!strictTypes && value instanceof String) try {
            if(((String) value).isEmpty())
                return null;
            return Float.parseFloat((String) value);
        } catch(NumberFormatException ignored) { }

        Number n = assertType(context, value, Float.class);
        if(value instanceof Double && n.floatValue() != n.doubleValue())
            throw new TypeMismatchException("Lossy conversion from Double to Float");
        return n.floatValue();
    }

    /**
     * Returns the value as {@code boolean}.
     *
     * @return The value as boolean
     * @throws TypeMismatchException If a value is present and
     *                            is not a boolean
     */
    protected Boolean asBool(JsonElement context, Object value) {
        if(!strictTypes && value instanceof String) {
            if(((String) value).equalsIgnoreCase("true"))
                return true;
            if(((String) value).equalsIgnoreCase("false"))
                return false;
            if(((String) value).isEmpty())
                return null;
        }
        return assertType(context, value, Boolean.class);
    }

    /**
     * Returns {@code true} if the value is null or convertible to a {@code boolean}.
     *
     * @return Whether the value is a boolean
     */
    protected boolean isBool(JsonElement context, Object value) {
        return value == null || value instanceof Boolean ||
               (!strictTypes && value instanceof String && (((String) value).isEmpty() || "true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value)));
    }

    protected JsonElement get(JsonElement context, Object value, String key) {
        return context.subElement(key, asObject(context, value).get(key));
    }

    protected JsonElement get(JsonElement context, Object value, int index) {
        return context.subElement(index, asArray(context, value).get(index));
    }

    protected Iterable<JsonElement> elements(JsonElement context, Object value) {
        if(value == null)
            return IterableIterator::empty;
        JsonArray arr = asArray(context, value); // Check type now
        return () -> new Iterator<>() {
            int index = 0;
            @Override
            public boolean hasNext() {
                return index < arr.size();
            }

            @Override
            public JsonElement next() {
                if(index >= arr.size())
                    throw new NoSuchElementException();
                return context.subElement(index, arr.get(index++));
            }
        };
    }

    protected void forEach(JsonElement context, Object value, BiConsumer<? super String, ? super JsonElement> action) {
        asObject(context, value).forEach((k,v) -> action.accept(k, context.subElement(k,v)));
    }

    protected Set<String> keySet(JsonElement context, Object value) {
        return asObject(context, value).keySet();
    }

    protected Collection<JsonElement> values(JsonElement context, Object value) {
        return asObject(context, value).entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(e -> context.subElement(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    protected Stream<JsonElement> stream(JsonElement context, Object value) {
        JsonArray array = asArray(context, value);
        return IntStream.range(0, array.size())
                .filter(i -> array.get(i) != null)
                .mapToObj(i -> context.subElement(i, array.get(i)));
    }

    protected int size(JsonElement context, Object value) {
        return asStructure(context, value).size();
    }



    /**
     * Returns a json deserializer which uses the given deserializer, but return null
     * if a deserialization fails, instead of throwing an exception.
     *
     * @param deserializer The deserializer to use internally
     * @return A deserializer that works like the given deserializer, except returning null if the other one fails
     */
    public static JsonDeserializer nullIfFail(JsonDeserializer deserializer) {
        return new NullIfFailDeserializer(deserializer);
    }


    private static class NullIfFailDeserializer extends JsonDeserializer {

        private final JsonDeserializer deserializer;

        public NullIfFailDeserializer(JsonDeserializer deserializer) {
            super(Objects.requireNonNull(deserializer, "deserializer").strictTypes);
            this.deserializer = deserializer;
        }

        @Override
        protected Object deserialize(Type type, JsonElement data, Object value) {
            try {
                return deserializer.deserialize(type, data, value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public JsonStructure asStructure(JsonElement context, Object value) {
            try {
                return deserializer.asStructure(context, value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public JsonObject asObject(JsonElement context, Object value) {
            try {
                return deserializer.asObject(context, value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public JsonArray asArray(JsonElement context, Object value) {
            try {
                return deserializer.asArray(context, value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public String asString(JsonElement context, Object value) {
            try {
                return deserializer.asString(context, value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Number asNumber(JsonElement context, Object value) {
            try {
                return deserializer.asNumber(context, value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Long asLong(JsonElement context, Object value) {
            try {
                return deserializer.asLong(context, value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Integer asInt(JsonElement context, Object value) {
            try {
                return deserializer.asInt(context, value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Double asDouble(JsonElement context, Object value) {
            try {
                return deserializer.asDouble(context, value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Float asFloat(JsonElement context, Object value) {
            try {
                return deserializer.asFloat(context, value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Boolean asBool(JsonElement context, Object value) {
            try {
                return deserializer.asBool(context, value);
            } catch(Exception e) {
                return null;
            }
        }
    }

    private static class BestEffortDeserializer extends JsonDeserializer {

        public BestEffortDeserializer() {
            super(true);
        }


        @Override
        public JsonStructure asStructure(JsonElement context, Object value) {
            if(value == null || value instanceof JsonStructure)
                return (JsonStructure) value;
            return new JsonArray(value);
        }

        @Override
        protected boolean isStructure(JsonElement context, Object value) {
            return true;
        }

        @Override
        public JsonArray asArray(JsonElement context, Object value) {
            if(value == null || value instanceof JsonArray)
                return (JsonArray) value;
            return new JsonArray(value);
        }

        @Override
        protected boolean isArray(JsonElement context, Object value) {
            return true;
        }

        @Override
        public String asString(JsonElement context, Object value) {
            return ""+context.get();
        }

        @Override
        protected boolean isString(JsonElement context, Object value) {
            return true;
        }

        @Override
        public Number asNumber(JsonElement context, Object value) {
            if(value == null) return null;

            if(value instanceof String) try {
                return Long.parseLong((String) value);
            } catch(NumberFormatException e) {
                try {
                    return Double.parseDouble((String) value);
                } catch(NumberFormatException ignored) { }
            }

            if(value instanceof Boolean)
                return (boolean) value ? 1 : 0;

            return assertType(context, value, Number.class);
        }

        @Override
        protected boolean isNumber(JsonElement context, Object value) {
            if(value == null || value instanceof Number || value instanceof Boolean)
                return true;
            if(value instanceof String) try {
                Double.parseDouble((String) value);
                return true;
            } catch(NumberFormatException ignored) { }
            return false;
        }

        @Override
        public Long asLong(JsonElement context, Object value) {
            if(value == null) return null;

            if(value instanceof String) try {
                return Long.parseLong((String) value);
            } catch(NumberFormatException ignored) { }

            if(value instanceof Boolean)
                return (boolean) value ? 1L : 0L;

            return assertType(context, value, Number.class).longValue();
        }

        @Override
        protected boolean isInteger(JsonElement context, Object value) {
            return isNumber(context, value);
        }

        @Override
        public Integer asInt(JsonElement context, Object value) {
            if(value == null) return null;

            if(value instanceof String) try {
                return Integer.parseInt((String) value);
            } catch(NumberFormatException ignored) { }

            if(value instanceof Boolean)
                return (boolean) value ? 1 : 0;

            return assertType(context, value, Number.class).intValue();
        }

        @Override
        public Double asDouble(JsonElement context, Object value) {
            if(value == null) return null;

            if(value instanceof String) try {
                return Double.parseDouble((String) value);
            } catch(NumberFormatException ignored) { }

            if(value instanceof Boolean)
                return (boolean) value ? 1.0 : 0.0;

            return assertType(context, value, Number.class).doubleValue();
        }

        @Override
        public Float asFloat(JsonElement context, Object value) {
            if(value == null) return null;

            if(value instanceof String) try {
                return Float.parseFloat((String) value);
            } catch(NumberFormatException ignored) { }

            if(value instanceof Boolean)
                return (boolean) value ? 1f : 0f;

            return assertType(context, value, Number.class).floatValue();
        }

        @Override
        public Boolean asBool(JsonElement context, Object value) {
            if(value instanceof Boolean)
                return (boolean) value;
            if(value instanceof String)
                return Boolean.parseBoolean((String) value);
            if(value instanceof Integer || value instanceof Long)
                return ((Number) value).longValue() != 0;
            if(value instanceof Float || value instanceof Double)
                return ((Number) value).doubleValue() != 0;
            return value != null;
        }

        @Override
        protected boolean isBool(JsonElement context, Object value) {
            return true;
        }
    }
}
