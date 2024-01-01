package de.rccookie.json;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * This class controls the "low level" deserialization and thus manages the level of allowed type conversion.
 */
public class JsonDeserializer {

    /**
     * A json deserializer which strictly enforces correct types.
     */
    public static final JsonDeserializer STRICT = new JsonDeserializer(true);
    /**
     * A json deserializer which allows numbers and boolean values to be enquoted as strings, and the other way around.
     */
    public static final JsonDeserializer STRING_CONVERSION = new JsonDeserializer(false);
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

    /**
     * Deserializes the given json data into the given type using
     * the mapped deserializer or the constructor of the type with
     * one argument of type {@link JsonElement}.
     *
     * @param type The type to deserialize to
     * @param data The data to deserialize
     * @return The deserialized value
     * @throws IllegalStateException If no deserializer is registered for
     *                               the specified type
     * @throws IllegalJsonDeserializerException If an exception occurs while loading the
     *                                          deserializer of the class to deserialize to
     */
    protected Object deserialize(Type type, JsonElement data) {
        return JsonSerialization.deserialize(type, data);
    }

    /**
     * Returns the value as {@link JsonStructure}.
     *
     * @return The value as json structure
     * @throws ClassCastException If a value is present and is not a json structure
     */
    protected JsonStructure asStructure(Object value) {
        return (JsonStructure) value;
    }

    /**
     * Returns the value as {@link JsonObject}.
     *
     * @return The value as json object
     * @throws ClassCastException If a value is present and is not a json object
     */
    protected JsonObject asObject(Object value) {
        return (JsonObject) value;
    }

    /**
     * Returns the value as {@link JsonArray}.
     *
     * @return The value as json array
     * @throws ClassCastException If al value is present and is not a json array
     */
    protected JsonArray asArray(Object value) {
        return (JsonArray) value;
    }

    /**
     * Returns the value as {@link String}.
     *
     * @return The value as string
     * @throws ClassCastException If a value is present and is not a string
     */
    protected String asString(Object value) {
        if(!strictTypes && (value instanceof Number || value instanceof Boolean))
            return ""+value;
        return (String) value;
    }

    /**
     * Returns the value as a number.
     *
     * @return The value as number
     * @throws ClassCastException If a value is present and is not a number
     */
    protected Number asNumber(Object value) {
        if(!strictTypes && value instanceof String) try {
            return Double.parseDouble((String) value);
        } catch(NumberFormatException ignored) { }
        return (Number) value;
    }

    /**
     * Returns the value as {@code long}.
     *
     * @return The value as long
     * @throws ClassCastException If a value is present and is not a number that can be converted to a long without data loss
     */
    protected Long asLong(Object value) {
        if(value == null) return null;

        if(!strictTypes && value instanceof String) try {
            return Long.parseLong((String) value);
        } catch(NumberFormatException ignored) { }

        Number n = (Number) value;
        if(value instanceof Long || value instanceof Integer || n.doubleValue() == n.longValue())
            return n.longValue();
        throw new ClassCastException("Lossy conversion from "+n.getClass().getSimpleName()+" to Long");
    }

    /**
     * Returns the value as {@code int}.
     *
     * @return The value as int
     * @throws ClassCastException If a value is present and
     *                            is not a number
     */
    protected Integer asInt(Object value) {
        if(value == null) return null;

        if(!strictTypes && value instanceof String) try {
            return Integer.parseInt((String) value);
        } catch(NumberFormatException ignored) { }

        Number n = (Number) value;
        if(value instanceof Integer || (value instanceof Long && n.longValue() == n.intValue())
           || ((value instanceof Double || value instanceof Float) && n.doubleValue() == n.intValue()))
            return n.intValue();
        throw new ClassCastException("Lossy conversion from "+n.getClass().getSimpleName()+" to Integer");
    }

    /**
     * Returns the value as {@code double}.
     *
     * @return The value as double
     * @throws ClassCastException If a value is present and
     *                            is not a number
     */
    protected Double asDouble(Object value) {
        return value != null ? asNumber(value).doubleValue() : null;
    }

    /**
     * Returns the value as {@code float}.
     *
     * @return The value as float
     * @throws ClassCastException If a value is present and
     *                            is not a number
     */
    protected Float asFloat(Object value) {
        if(value == null) return null;

        if(!strictTypes && value instanceof String) try {
            return Float.parseFloat((String) value);
        } catch(NumberFormatException ignored) { }

        Number n = (Number) value;
        if(value instanceof Double && n.floatValue() != n.doubleValue())
            throw new ClassCastException("Lossy conversion from Double to Float");
        return n.floatValue();
    }

    /**
     * Returns the value as {@code boolean}.
     *
     * @return The value as boolean
     * @throws ClassCastException If a value is present and
     *                            is not a boolean
     */
    protected Boolean asBool(Object value) {
        if(!strictTypes && value instanceof String) {
            if(((String) value).equalsIgnoreCase("true"))
                return true;
            if(((String) value).equalsIgnoreCase("false"))
                return false;
        }
        return (Boolean) value;
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
        protected Object deserialize(Type type, JsonElement data) {
            try {
                return deserializer.deserialize(type, data);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public JsonStructure asStructure(Object value) {
            try {
                return deserializer.asStructure(value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public JsonObject asObject(Object value) {
            try {
                return deserializer.asObject(value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public JsonArray asArray(Object value) {
            try {
                return deserializer.asArray(value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public String asString(Object value) {
            try {
                return deserializer.asString(value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Number asNumber(Object value) {
            try {
                return deserializer.asNumber(value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Long asLong(Object value) {
            try {
                return deserializer.asLong(value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Integer asInt(Object value) {
            try {
                return deserializer.asInt(value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Double asDouble(Object value) {
            try {
                return deserializer.asDouble(value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Float asFloat(Object value) {
            try {
                return deserializer.asFloat(value);
            } catch(Exception e) {
                return null;
            }
        }

        @Override
        public Boolean asBool(Object value) {
            try {
                return deserializer.asBool(value);
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
        public JsonStructure asStructure(Object value) {
            if(value == null || value instanceof JsonStructure)
                return (JsonStructure) value;
            return new JsonArray(value);
        }

        @Override
        public JsonArray asArray(Object value) {
            if(value == null || value instanceof JsonArray)
                return (JsonArray) value;
            return new JsonArray(value);
        }

        @Override
        public String asString(Object value) {
            return ""+value;
        }

        @Override
        public Number asNumber(Object value) {
            if(value == null) return null;

            if(value instanceof String) try {
                return Double.parseDouble((String) value);
            } catch(NumberFormatException ignored) { }

            if(value instanceof Boolean)
                return (boolean) value ? 1 : 0;

            //noinspection DataFlowIssue
            return (Number) value;
        }

        @Override
        public Long asLong(Object value) {
            if(value == null) return null;

            if(value instanceof String) try {
                return Long.parseLong((String) value);
            } catch(NumberFormatException ignored) { }

            if(value instanceof Boolean)
                return (boolean) value ? 1L : 0L;

            //noinspection DataFlowIssue
            return ((Number) value).longValue();
        }

        @Override
        public Integer asInt(Object value) {
            if(value == null) return null;

            if(value instanceof String) try {
                return Integer.parseInt((String) value);
            } catch(NumberFormatException ignored) { }

            if(value instanceof Boolean)
                return (boolean) value ? 1 : 0;

            //noinspection DataFlowIssue
            return ((Number) value).intValue();
        }

        @Override
        public Double asDouble(Object value) {
            if(value == null) return null;

            if(value instanceof String) try {
                return Double.parseDouble((String) value);
            } catch(NumberFormatException ignored) { }

            if(value instanceof Boolean)
                return (boolean) value ? 1.0 : 0.0;

            //noinspection DataFlowIssue
            return ((Number) value).doubleValue();
        }

        @Override
        public Float asFloat(Object value) {
            if(value == null) return null;

            if(value instanceof String) try {
                return Float.parseFloat((String) value);
            } catch(NumberFormatException ignored) { }

            if(value instanceof Boolean)
                return (boolean) value ? 1f : 0f;

            //noinspection DataFlowIssue
            return ((Number) value).floatValue();
        }

        @Override
        public Boolean asBool(Object value) {
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
    }
}
