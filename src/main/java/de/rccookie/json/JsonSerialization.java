package de.rccookie.json;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

final class JsonSerialization {

    private JsonSerialization() { }


    private static final Unsafe UNSAFE;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch(NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static final Function<JsonSerializable,?> JSON_SERIALIZABLE_SERIALIZER = JsonSerializable::toJson;
    private static final Function<?,JsonArray> ARRAY_SERIALIZER = arr -> {
        JsonArray json = new JsonArray();
        int length = Array.getLength(arr);
        for(int i=0; i<length; i++)
            json.add(Array.get(arr, i)); // add() calls serialize() internally
        return json;
    };
    private static final Function<Enum<?>,?> DEFAULT_ENUM_SERIALIZER = Enum::name;


    private static final Supplier<?> NULL_SUPPLIER = () -> null;


    private static final Map<Class<?>, Function<?,?>> INTERFACE_SERIALIZERS = new HashMap<>();
    private static final Map<Class<?>, Function<?,?>> CONCRETE_SERIALIZERS = new HashMap<>();
    private static final Map<Class<?>, Function<?,?>> FIXED_SERIALIZERS = new HashMap<>();
    private static final Map<Class<?>, Function<JsonElement, ?>> DESERIALIZERS = new HashMap<>();
    private static final Map<Class<?>, Function<JsonElement,?>> FIXED_DESERIALIZERS = new HashMap<>();

    // Default serializers and deserializers
    static {
        DESERIALIZERS.put(Boolean.class,         JsonElement::asBool);
        DESERIALIZERS.put(boolean.class,         JsonElement::asBool);
        DESERIALIZERS.put(Number.class,          JsonElement::asNumber);
        DESERIALIZERS.put(Byte.class,            j -> j.asNumber().byteValue());
        DESERIALIZERS.put(byte.class,            j -> j.asNumber().byteValue());
        DESERIALIZERS.put(Short.class,           j -> j.asNumber().shortValue());
        DESERIALIZERS.put(short.class,           j -> j.asNumber().shortValue());
        DESERIALIZERS.put(Integer.class,         JsonElement::asInt);
        DESERIALIZERS.put(int.class,             JsonElement::asInt);
        DESERIALIZERS.put(Long.class,            JsonElement::asLong);
        DESERIALIZERS.put(long.class,            JsonElement::asLong);
        DESERIALIZERS.put(Float.class,           JsonElement::asFloat);
        DESERIALIZERS.put(float.class,           JsonElement::asFloat);
        DESERIALIZERS.put(Double.class,          JsonElement::asDouble);
        DESERIALIZERS.put(double.class,          JsonElement::asDouble);
        DESERIALIZERS.put(String.class,          JsonElement::asString);
        DESERIALIZERS.put(JsonStructure.class,   JsonElement::asStructure);
        DESERIALIZERS.put(JsonObject.class,      JsonElement::asObject);
        DESERIALIZERS.put(JsonArray.class,       JsonElement::asArray);
        DESERIALIZERS.put(JsonElement.class,     j -> j);
        DESERIALIZERS.put(Character.class,       j -> j.asString().charAt(0));
        DESERIALIZERS.put(char.class,            j -> j.asString().charAt(0));
        registerSerializer(Character.class,      c -> c+"");
        registerSerializer(char.class,           c -> c+"");

        registerSerializer(Object.class, $ -> new JsonObject());
        registerDeserializer(Object.class, JsonElement::get);

        registerSerializer(Class.class, Class::getName);
        registerDeserializer(Class.class, j -> {try{ return Class.forName(j.asString(), false, null); }catch(ClassNotFoundException e){ throw new RuntimeException(e); }});
        registerSerializer(Void.class, $ -> null);
        registerDeserializer(Void.class, $ -> null);
        registerSerializer(StringBuilder.class, StringBuilder::toString);
        registerDeserializer(StringBuilder.class, j -> new StringBuilder(j.asString()));
        registerSerializer(StringBuffer.class, StringBuffer::toString);
        registerDeserializer(StringBuffer.class, j -> new StringBuffer(j.asString()));

        registerSerializer(Collection.class, JsonArray::new);
        registerSerializer(Map.Entry.class, e -> new JsonObject("key", e.getKey(), "value", e.getValue()));
        registerSerializer(Map.class, map -> {
            Map<Object, Object> serialized = new HashMap<>();
            //noinspection unchecked
            map.forEach((k,v) -> serialized.put(serialize(k), serialize(v)));
            if(serialized.keySet().stream().allMatch(String.class::isInstance))
                return new JsonObject(serialized);
            return serialized.entrySet();
        });

        FIXED_SERIALIZERS.putAll(CONCRETE_SERIALIZERS);
        FIXED_SERIALIZERS.putAll(INTERFACE_SERIALIZERS);
        FIXED_DESERIALIZERS.putAll(DESERIALIZERS);

        registerSerializer(Process.class, Process::pid);
        registerSerializer(ProcessHandle.class, ProcessHandle::pid);
        registerDeserializer(ProcessHandle.class, j -> ProcessHandle.of(j.asLong()).orElse(null));

        registerSerializer(File.class, File::getPath);
        registerDeserializer(File.class, j -> new File(j.asString()));
        registerSerializer(Path.class, Object::toString);
        registerDeserializer(Path.class, j -> Path.of(j.asString()));
        registerSerializer(URL.class, URL::toString);
        registerDeserializer(URL.class, j -> {try{ return new URL(j.asString()); }catch(MalformedURLException e){ throw new RuntimeException(e); }});
        registerSerializer(URI.class, URI::toString);
        registerDeserializer(URI.class, j -> URI.create(j.asString()));
        registerSerializer(UUID.class, Object::toString);
        registerDeserializer(UUID.class, j -> UUID.fromString(j.asString()));
        registerSerializer(Date.class, Date::getTime);
        registerDeserializer(Date.class, j -> new Date(j.asLong()));
        //noinspection unchecked
        registerSerializer(Optional.class, o -> o.orElse(null));
        // Generic - no deserializer - can be done using JsonElement class
        registerSerializer(OptionalInt.class, o -> o.isPresent() ? o.getAsInt() : null);
        registerDeserializer(OptionalInt.class, j -> j.isPresent() ? OptionalInt.of(j.asInt()) : OptionalInt.empty());
        registerSerializer(OptionalLong.class, (OptionalLong o) -> o.isPresent() ? o.getAsLong() : null);
        registerDeserializer(OptionalLong.class, j -> j.isPresent() ? OptionalLong.of(j.asLong()) : OptionalLong.empty());
        registerSerializer(OptionalDouble.class, (OptionalDouble o) -> o.isPresent() ? o.getAsDouble() : null);
        registerDeserializer(OptionalDouble.class, j -> j.isPresent() ? OptionalDouble.of(j.asInt()) : OptionalDouble.empty());
        registerSerializer(Instant.class, (Instant i) -> new JsonObject(
                "epochSecond", i.getEpochSecond(),
                "nano", i.getNano()
        ));
        registerDeserializer(Instant.class, j -> Instant.ofEpochSecond(
                j.get("epochSecond").asLong(),
                j.get("nano").asInt()
        ));
        registerSerializer(LocalTime.class, (LocalTime t) -> new JsonObject(
                "hour", t.getHour(),
                "minute", t.getMinute(),
                "second", t.getSecond(),
                "nano", t.getNano()
        ));
        registerDeserializer(LocalTime.class, j -> LocalTime.of(
                j.get("hour").asInt(),
                j.get("minute").asInt(),
                j.get("second").asInt(),
                j.get("nano").asInt()
        ));
        registerSerializer(OffsetTime.class, (OffsetTime t) -> new JsonObject(
                "localTime", t.toLocalTime(),
                "offset", t.getOffset()
        ));
        registerDeserializer(OffsetTime.class, j -> OffsetTime.of(
                j.get("localTime").as(LocalTime.class),
                j.get("offset").as(ZoneOffset.class)
        ));
        registerSerializer(LocalDate.class, (LocalDate d) -> new JsonObject(
                "year", d.getYear(),
                "month", d.getMonthValue(),
                "day", d.getDayOfMonth()
        ));
        registerDeserializer(LocalDate.class, j -> LocalDate.of(
                j.get("year").asInt(),
                j.get("month").asInt(),
                j.get("day").asInt()
        ));
        registerSerializer(ZoneOffset.class, ZoneOffset::getTotalSeconds);
        registerDeserializer(ZoneOffset.class, j -> ZoneOffset.ofTotalSeconds(j.asInt()));
        registerSerializer(ZoneId.class, (ZoneId z) -> z instanceof ZoneOffset ?
                ((ZoneOffset) z).getTotalSeconds() : z.getId()
        );
        registerDeserializer(ZoneId.class, j -> j.isString() ? ZoneId.of(j.asString()) : ZoneOffset.ofTotalSeconds(j.asInt()));
        registerSerializer(LocalDateTime.class, (LocalDateTime dt) -> new JsonObject(
                "date", dt.toLocalDate(),
                "time", dt.toLocalTime()
        ));
        registerDeserializer(LocalDateTime.class, j -> LocalDateTime.of(
                j.get("date").as(LocalDate.class),
                j.get("time").as(LocalTime.class)
        ));
        registerSerializer(ZonedDateTime.class, (ZonedDateTime dt) -> new JsonObject(
                "dateTime", dt.toLocalDateTime(),
                "zone", dt.getZone()
        ));
        registerDeserializer(ZonedDateTime.class, j -> ZonedDateTime.of(
                j.get("dateTime").as(LocalDateTime.class),
                j.get("zone").as(ZoneId.class)
        ));
        registerSerializer(OffsetDateTime.class, (OffsetDateTime dt) -> new JsonObject(
                "dateTime", dt.toLocalDateTime(),
                "offset", dt.getOffset()
        ));
        registerDeserializer(OffsetDateTime.class, j -> OffsetDateTime.of(
                j.get("dateTime").as(LocalDateTime.class),
                j.get("offset").as(ZoneOffset.class)
        ));
        registerSerializer(Duration.class, (Duration d) -> new JsonObject(
                "seconds", d.getSeconds(),
                "nano", d.getNano()
        ));
        registerDeserializer(Duration.class, j -> Duration.ofSeconds(
                j.get("seconds").asLong(),
                j.get("nano").asInt()
        ));
        registerSerializer(MonthDay.class, (MonthDay d) -> new JsonObject(
                "month", d.getMonthValue(),
                "day", d.getDayOfMonth()
        ));
        registerDeserializer(MonthDay.class, j -> MonthDay.of(
                j.get("month").asInt(),
                j.get("day").asInt()
        ));
        registerSerializer(YearMonth.class, (YearMonth m) -> new JsonObject(
                "year", m.getYear(),
                "month", m.getMonthValue()
        ));
        registerDeserializer(YearMonth.class, j -> YearMonth.of(
                j.get("year").asInt(),
                j.get("month").asInt()
        ));
        registerSerializer(Period.class, (Period p) -> new JsonObject(
                "years", p.getYears(),
                "months", p.getMonths(),
                "days", p.getDays()
        ));
        registerDeserializer(Period.class, j -> Period.of(
                j.get("years").asInt(),
                j.get("months").asInt(),
                j.get("days").asInt()
        ));
    }


    /**
     * Registers a custom serializer for the specified type.
     *
     * @param type The type the serializer serializes from
     * @param serializer The json serializer
     */
    static <T> void registerSerializer(Class<T> type, Function<? super T, ?> serializer) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(serializer, "serializer");
        if(type.isArray())
            throw new IllegalArgumentException("Cannot override array type serialization");
        if(isJsonType(type))
            throw new IllegalArgumentException("Cannot override serializer for raw json type");
        if(JsonSerializable.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Cannot override serializer for JsonSerializable subclass");
        if(FIXED_SERIALIZERS.containsKey(type))
            throw new IllegalArgumentException("Cannot override serializer for "+type);

        if(type.isInterface())
            INTERFACE_SERIALIZERS.put(type, serializer);
        else CONCRETE_SERIALIZERS.put(type, serializer);
    }

    /**
     * Registers a custom deserializer for the specified type.
     *
     * @param type The type the deserializer deserializes to
     * @param deserializer The json deserializer
     */
    static <T> void registerDeserializer(Class<T> type, Function<JsonElement, ? extends T> deserializer) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(deserializer, "deserializer");

        if(type.isArray())
            throw new IllegalArgumentException("Cannot override array type deserialization");
        if(type.isPrimitive() || type == String.class)
            throw new IllegalArgumentException("Cannot override serializer for raw json type");
        if(FIXED_DESERIALIZERS.containsKey(type))
            throw new IllegalArgumentException("Cannot override serializer for "+type);

        DESERIALIZERS.put(type, deserializer);
    }

    @SuppressWarnings("unchecked")
    static Object serialize(Object obj) {
        while(!isJsonTypeInstance(obj))
            obj = getSerializer(obj.getClass()).apply(obj);
        return obj;
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
    @SuppressWarnings("unchecked")
    static <T> T deserialize(Class<T> type, JsonElement data) {
        if(data.isEmpty())
            return type.isArray() ? (T) Array.newInstance(type.getComponentType(), 0) : null;
        try {
            return getDeserializer(type).apply(data);
        } catch(IllegalJsonDeserializerException e) {
            throw e;
        } catch(RuntimeException e) {
            throw new JsonDeserializationException(data, type, e);
        }
    }



    @SuppressWarnings("rawtypes")
    private static Function getSerializer(Class<?> type) {

        // Does the class implement JsonSerializable?
        if(JsonSerializable.class.isAssignableFrom(type))
            return JSON_SERIALIZABLE_SERIALIZER;

        // Arrays are handled separately
        if(type.isArray())
            return ARRAY_SERIALIZER;

        // The type does not need to be loaded since we already have an instance (the one we want to serialize)

        // Look up (possibly cached) serializer. Since the type must always be a concrete type (as an instance
        // of the type exists), we can use CONCRETE_SERIALIZERS as cache
        Function serializer = CONCRETE_SERIALIZERS.get(type);
        if(serializer != null) return serializer;

        serializer = findSerializer(type);
        // Cache with concrete type for faster subsequent lookup
        CONCRETE_SERIALIZERS.put(type, serializer);
        return serializer;
    }

    @NotNull
    private static Function<?,?> findSerializer(Class<?> type) {

        // Try to find serializer of supertype
        for(Class<?> superType = type.getSuperclass(); superType != Object.class; superType = superType.getSuperclass())
            if(CONCRETE_SERIALIZERS.containsKey(superType))
                return CONCRETE_SERIALIZERS.get(superType);

        // Try to find serializer of implemented interface
        for(Class<?> interfaceType : getImplementedInterfaces(type))
            if(INTERFACE_SERIALIZERS.containsKey(interfaceType))
                return INTERFACE_SERIALIZERS.get(interfaceType);

        // Check for enum AFTER searching for custom serializer
        if(type.isEnum())
            return DEFAULT_ENUM_SERIALIZER;

        // Check for record also after searching for custom serializer
        if(isRecord(type))
            return generateRecordSerializer(type);

        if(!type.isInterface() && !Modifier.isAbstract(type.getModifiers()))
            return generateUnsafeSerializer(type);

        throw new IllegalJsonTypeException(type);
    }

    private static Function<?,?> generateRecordSerializer(Class<?> type) {
        try {
            List<BiConsumer<Object, JsonObject>> readers = new ArrayList<>();
            for(Field f : type.getDeclaredFields()) {
                if(Modifier.isStatic(f.getModifiers())) continue;

                String name = f.getName();
                Method accessor = type.getMethod(name);
                accessor.setAccessible(true);
                readers.add((obj,json) -> {try {
                    json.put(name, serialize(accessor.invoke(obj)));
                } catch(IllegalAccessException e) {
                    throw new AssertionError(e);
                } catch(InvocationTargetException e) {
                    if(e.getTargetException() instanceof RuntimeException)
                        throw (RuntimeException) e.getTargetException();
                    throw new RuntimeException(e.getTargetException());
                }});
            }
            return obj -> {
                JsonObject json = new JsonObject();
                for(BiConsumer<Object, JsonObject> reader : readers)
                    reader.accept(obj, json);
                return json;
            };
        } catch(NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Function<?,?> generateUnsafeSerializer(Class<?> type) {
        Field[] fields = Arrays.stream(type.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).toArray(Field[]::new);
        Function<Object,?>[] getters = new Function[fields.length];
        for(int i=0; i<fields.length; i++)
            getters[i] = getUnsafeGetter(fields[i]);

        return obj -> {
            JsonObject json = new JsonObject();
            for(int i=0; i<getters.length; i++)
                json.put(fields[i].getName(), serialize(getters[i].apply(obj)));
            return json;
        };
    }



    @SuppressWarnings({"unchecked", "DuplicatedCode", "rawtypes"})
    private static <T> Function<JsonElement, T> getDeserializer(Class<T> type) throws IllegalJsonDeserializerException {
        if(type == Object.class)
            throw new IllegalJsonDeserializerException("Cannot deserialize to java.lang.Object");

        // Deserialize to array component-wise
        if(type.isArray()) {
            Class<?> contentType = type.getComponentType();
            return json -> {
                T arr = (T) Array.newInstance(type.getComponentType(), json.size());
                for(int i=0; i<json.size(); i++)
                    Array.set(arr, i, json.get(i).as(contentType));
                return arr;
            };
        }

        // Initialize class giving a chance to register a deserializer
        loadType(type);

        // Check for custom or cached deserializer
        Function<JsonElement, T> deserializer = (Function<JsonElement, T>) DESERIALIZERS.get(Objects.requireNonNull(type));
        if(deserializer != null) return deserializer;

        // Check for enum AFTER searching for custom deserializer
        if(type.isEnum())
            return generateEnumDeserializer((Class) type);

        Constructor<T> ctor = (Constructor<T>) Arrays.stream(type.getDeclaredConstructors())
                .peek(c -> c.setAccessible(true))
                .filter(c -> c.isAnnotationPresent(JsonCtor.class))
                .findAny()
                .orElse(null);

        if(ctor == null) {
            try {
                Constructor<T> altCtor = type.getDeclaredConstructor(/* paramType: */ JsonElement.class);
                altCtor.setAccessible(true);
                deserializer = j -> {
                    try { return altCtor.newInstance(j); }
                    catch(Exception e) { throw new RuntimeException(e); }
                };
                DESERIALIZERS.put(type, deserializer);
                return deserializer;
            } catch(NoSuchMethodException e) {
                if(isRecord(type)) {
                    deserializer = getRecordDeserializer(type);
                    DESERIALIZERS.put(type, deserializer);
                    return deserializer;
                }
                if(!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                    deserializer = getUnsafeDeserializer(type);
                    DESERIALIZERS.put(type, deserializer);
                    return deserializer;
                }

                throw new IllegalJsonDeserializerException("The type " + type.getSimpleName() + " is not marked from json deserialization");
            }
        }

        Supplier<?>[] defaults = getDefaults(ctor);
        JsonCtor props = ctor.getAnnotation(JsonCtor.class);
        Class<?>[] paramTypes = ctor.getParameterTypes();
        Type packaging = props.type();

        // Json object packaged
        if(packaging == Type.OBJECT || (packaging == Type.AUTO && props.value().length != 0)) {
            String[] keys = props.value();
            if(keys.length != paramTypes.length)
                throw new IllegalJsonDeserializerException("The annotated constructor of "+type.getSimpleName()+" has the wrong number of parameters");
            deserializer = j -> {
                Object[] params = new Object[keys.length];
                for(int i=0; i<params.length; i++) {
                    JsonElement jParam = j.get(keys[i]);
                    if(jParam.isPresent())
                        params[i] = jParam.as(paramTypes[i]);
                    else if(defaults[i] != null)
                        params[i] = defaults[i].get();
                    else throw new MissingFieldException(paramTypes[i], "'" + keys[i] + "'");
                }
                try { return ctor.newInstance(params); }
                catch(Exception e) { throw new RuntimeException(e); }
            };
        }
        // Json array packaged
        else if(packaging == Type.ARRAY || (packaging == Type.AUTO && props.indices().length != 0)) {
            int[] indices;
            if(props.indices().length == 0) {
                // No indices specified -> use parameter indices
                indices = new int[paramTypes.length];
                Arrays.setAll(indices, IntUnaryOperator.identity());
            }
            else {
                indices = props.indices();
                if(indices.length != paramTypes.length)
                    throw new IllegalJsonDeserializerException("The annotated constructor of " + type.getSimpleName() + " has the wrong number of parameters");
            }
            deserializer = j -> {
                Object[] params = new Object[indices.length];
                for(int i=0; i<params.length; i++) {
                    JsonElement jParam = j.get(indices[i]);
                    if(jParam.isPresent())
                        params[i] = jParam.as(paramTypes[i]);
                    else if(defaults[i] != null)
                        params[i] = defaults[i].get();
                    else throw new MissingFieldException(paramTypes[i], "[" + indices[i] + "]");
                }
                try { return ctor.newInstance(params); }
                catch (Exception e) { throw new RuntimeException(e); }
            };
        }
        // Not packaged
        else {
            if(paramTypes.length != 1)
                throw new IllegalJsonDeserializerException("The annotated json of "+type.getSimpleName()+" constructor may have exactly one parameter");
            Function<JsonElement,?> paramDeserializer = getDeserializer(paramTypes[0]); // Preload deserializer and throw exceptions now rather than during actual deserialization
            deserializer = j -> {
                Object param;
                if(j.isEmpty())
                    param = paramTypes[0].isArray() ? (T) Array.newInstance(paramTypes[0].getComponentType(), 0) : null;
                else try {
                    param = paramDeserializer.apply(j);
                } catch(RuntimeException e) {
                    throw new JsonDeserializationException(j, paramTypes[0], e);
                }
                try { return ctor.newInstance(param); }
                catch(Exception e) { throw new RuntimeException(e); }
            };
        }
        CONCRETE_SERIALIZERS.put(type, deserializer);
        return deserializer;
    }

    private static <T> Function<JsonElement,T> getRecordDeserializer(Class<T> type) {
        Field[] fields = Arrays.stream(type.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).toArray(Field[]::new);
        Class<?>[] types = new Class[fields.length];
        for(int i=0; i<fields.length; i++)
            types[i] = fields[i].getType();
        try {
            Constructor<T> ctor = type.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            Supplier<?>[] defaults = getDefaults(ctor);

            if(types.length == 1) return json -> {
                try {
                    Object param;
                    try {
                        JsonElement jParam = json.get(fields[0].getName());
                        if(jParam.isPresent())
                            param = jParam.as(types[0]);
                        else if(defaults[0] != null)
                            param = defaults[0].get();
                        else throw new MissingFieldException(types[0], "'" + fields[0].getName() + "'");
                    } catch(RuntimeException e) {
                        // Try to deserialize as if serialized unpacked for backwards compatability
                        try {
                            // No need to test for default value: if no value was given, json.isPresent() would return false,
                            // and so would json.get(...).isPresent(). We already checked that above.
                            param = json.as(types[0]);
                            System.err.println("Warning: Json uses deprecated record serialization format");
                        } catch(RuntimeException f) {
                            e.addSuppressed(f);
                            throw e;
                        }
                    }
                    return ctor.newInstance(param);
                } catch(InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch(InstantiationException | IllegalAccessException e) {
                    throw new AssertionError(e); // Access: Should have failed during setAccessible()
                }
            };

            return json -> {
                try {
                    Object[] args = new Object[types.length];
                    for(int i=0; i<args.length; i++) {
                        JsonElement jParam = json.get(fields[i].getName());
                        if(jParam.isPresent())
                            args[i] = jParam.as(types[i]);
                        else if(defaults[i] != null)
                            args[i] = defaults[i].get();
                        else throw new MissingFieldException(types[i], "'" + fields[i].getName() + "'");
                    }
                    return ctor.newInstance(args);
                } catch(InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch(InstantiationException | IllegalAccessException e) {
                    throw new AssertionError(e); // Access: Should have failed during setAccessible()
                }
            };
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<JsonElement, T> getUnsafeDeserializer(Class<T> type) {
        Field[] fields = Arrays.stream(type.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).toArray(Field[]::new);
        BiConsumer<Object, Object>[] setters = new BiConsumer[fields.length];
        for(int i=0; i<fields.length; i++)
            setters[i] = getUnsafeSetter(fields[i]);
        Supplier<?>[] defaults = getDefaults(fields);

        return json -> {
            try {
                T t = (T) UNSAFE.allocateInstance(type);
                for(int i=0; i<fields.length; i++) {
                    JsonElement jField = json.get(fields[i].getName());
                    Object val;
                    if(jField.isPresent())
                        val = jField.as(fields[i].getType());
                    else if(defaults[i] != null)
                        val = defaults[i].get();
                    else throw new MissingFieldException(fields[i].getType(), "'"+fields[i].getName()+"'");
                    setters[i].accept(t, val);
                }
                return t;
            } catch(InstantiationException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Supplier<?>[] getDefaults(Constructor<?> ctor) {
        Class<?>[] params = ctor.getParameterTypes();
        Annotation[][] annotations = ctor.getParameterAnnotations();
        Supplier<?>[] defaults = new Supplier<?>[params.length];

        for(int i=0; i<params.length; i++) try {
            Supplier<?> supplier = null;
            boolean required = false;
            for(Annotation a : annotations[i]) {
                if(a instanceof Default)
                    supplier = parseDefault((Default) a, params[i]);
                else required |= a instanceof Required;
            }
            if(supplier != null) {
                if(required)
                    throw new IllegalJsonDeserializerException(ctor+": parameter "+(i+1)+": Cannot be required and have a default value");
            }
            else if(!required && !params[i].isPrimitive())
                supplier = NULL_SUPPLIER;

            defaults[i] = supplier;
        } catch(IllegalDefaultValueException e) {
            throw new IllegalDefaultValueException(ctor+": parameter "+(i+1)+": "+e.getMessage(), e);
        }

        return defaults;
    }

    private static Supplier<?>[] getDefaults(Field[] fields) {
        Supplier<?>[] defaults = new Supplier<?>[fields.length];

        for(int i=0; i<fields.length; i++) try {
            Supplier<?> supplier = null;
            boolean required = false;
            for(Annotation a : fields[i].getAnnotations()) {
                if(a instanceof Default)
                    supplier = parseDefault((Default) a, fields[i].getType());
                else required |= a instanceof Required;
            }
            if(supplier != null) {
                if(required)
                    throw new IllegalJsonDeserializerException(fields[i]+": Cannot be required and have a default value");
            }
            else if(!required && !fields[i].getType().isPrimitive())
                supplier = NULL_SUPPLIER;

            defaults[i] = supplier;
        } catch(IllegalDefaultValueException e) {
            throw new IllegalDefaultValueException(fields[i]+": "+e.getMessage(), e);
        }

        return defaults;
    }


    private static <E extends Enum<E>> Function<JsonElement,E> generateEnumDeserializer(Class<E> enumType) {
        return new Function<>() {
            final E[] values = enumType.getEnumConstants();
            @Override
            public E apply(JsonElement json) {
                if(json.isInteger())
                    return values[json.asInt()];
                return Enum.valueOf(enumType, json.asString());
            }
        };
    }

    /**
     * Tests if the given object is of a valid "raw" json type.
     *
     * @param o The object to check
     * @return Whether the object is of a valid json type
     */
    private static boolean isJsonTypeInstance(Object o) {
        return o == null ||
                o instanceof Number ||
                o instanceof String ||
                o instanceof Boolean ||
                o instanceof JsonStructure;
    }

    private static boolean isJsonType(Class<?> type) {
        return JsonStructure.class.isAssignableFrom(type) ||
                type == String.class ||
                type == boolean.class || type == Boolean.class ||
                type == int.class || type == Integer.class ||
                type == long.class || type == Long.class ||
                type == float.class || type == Float.class ||
                type == double.class || type == Double.class ||
                type == byte.class || type == Byte.class ||
                type == short.class || type == Short.class;
    }

    private static void loadType(Class<?> type) {
        if(type.isPrimitive()) return;
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch(ClassNotFoundException e) {
            throw new AssertionError();
        }
    }

    private static Set<Class<?>> getImplementedInterfaces(Class<?> type) {
        Set<Class<?>> interfaces = new HashSet<>();
        for(; type != Object.class; type = type.getSuperclass())
            addImplementedInterfaces(type, interfaces);
        return interfaces;
    }

    private static void addImplementedInterfaces(Class<?> type, Set<Class<?>> set) {
        for(Class<?> i : type.getInterfaces())
            if(set.add(i))
                addImplementedInterfaces(i, set);
    }

    private static boolean isRecord(Class<?> type) {
        // Cannot use type.isRecord() to be able to compile to lower class versions
        return type.getSuperclass().getName().equals("java.lang.Record");
    }

//    private static Supplier<?> parseDefault(Default d, Class<?> type) {
//        String value = d.value();
//        if(type.isArray()) {
//            Supplier<?>[] suppliers = new Supplier<?>[values.length];
//            for(int i=0; i<values.length; i++)
//                suppliers[i] = parseDefault(values[i], type.getComponentType(), d);
//            return () -> {
//                Object arr = Array.newInstance(type.getComponentType(), suppliers.length);
//                for(int i=0; i<suppliers.length; i++)
//                    Array.set(arr, i, suppliers[i].get());
//                return arr;
//            };
//        }
//        if(type == Collection.class || type == Set.class || type == List.class) {
//            Class<?> contentType = getValueType(d);
//            List<Supplier<?>> suppliers = new ArrayList<>();
//            for(String value : values)
//                suppliers.add(parseDefault(value, contentType, d));
//            if(type == Set.class)
//                return () -> suppliers.stream().map(Supplier::get).collect(Collectors.toSet());
//            else return () -> suppliers.stream().map(Supplier::get).collect(Collectors.toList());
//        }
//        if(values.length != 1)
//            throw new IllegalDefaultValueException("Parameter has non-array or collection type "+type.getSimpleName()+"; default value should not be array");
//        return parseDefault(values[0], type, d);
//    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Supplier<?> parseDefault(Default d, Class<?> type) {
        String value = d.value();
        if(type.isEnum()) {
            Object val = !d.string() && value.startsWith("\"") ? Json.parse(value).as(type) : Enum.valueOf((Class) type, value);
            return () -> val;
        }
        if(type == String.class) {
            String str = d.string() ? value : Json.parse(value).asString();
            return () -> str;
        }

        if(value.equals("null")) return NULL_SUPPLIER;
        JsonElement json = d.string() ? JsonElement.wrap(value) : Json.parse(value);
        if(type == List.class || type == Collection.class)
            return () -> json.asList(getValueType(d));
        if(type == Set.class)
            return () -> json.asSet(getValueType(d));
        if(type == Map.class)
            return () -> json.asMap(d.keyType(), getValueType(d));
        return () -> json.as(type); // Also correct if type.isArray()
    }

    private static Class<?> getValueType(Default d) {
        if(d.valueType() == void.class)
            throw new IllegalDefaultValueException("The 'valueType' parameter is required for the default value of a Collection or Map");
        return d.valueType();
    }

    private static Function<Object,?> getUnsafeGetter(Field field) {
        long offset = UNSAFE.objectFieldOffset(field);
        boolean isVolatile = Modifier.isVolatile(field.getModifiers());
        Class<?> type = field.getType();
        if(type.equals(boolean.class))
            return isVolatile ? o -> UNSAFE.getBooleanVolatile(o, offset) : o -> UNSAFE.getBoolean(o, offset);
        if(type.equals(byte.class))
            return isVolatile ? o -> UNSAFE.getByteVolatile(o, offset) : o -> UNSAFE.getByte(o, offset);
        if(type.equals(short.class))
            return isVolatile ? o -> UNSAFE.getShortVolatile(o, offset) : o -> UNSAFE.getShort(o, offset);
        if(type.equals(int.class))
            return isVolatile ? o -> UNSAFE.getIntVolatile(o, offset) : o -> UNSAFE.getInt(o, offset);
        if(type.equals(long.class))
            return isVolatile ? o -> UNSAFE.getLongVolatile(o, offset) : o -> UNSAFE.getLong(o, offset);
        if(type.equals(float.class))
            return isVolatile ? o -> UNSAFE.getFloatVolatile(o, offset) : o -> UNSAFE.getFloat(o, offset);
        if(type.equals(double.class))
            return isVolatile ? o -> UNSAFE.getDoubleVolatile(o, offset) : o -> UNSAFE.getDouble(o, offset);
        if(type.equals(char.class))
            return isVolatile ? o -> UNSAFE.getCharVolatile(o, offset) : o -> UNSAFE.getChar(o, offset);
        else return isVolatile ? o -> UNSAFE.getObjectVolatile(o, offset) : o -> UNSAFE.getObject(o, offset);
    }

    private static BiConsumer<Object,Object> getUnsafeSetter(Field field) {
        long offset = UNSAFE.objectFieldOffset(field);
        boolean isVolatile = Modifier.isVolatile(field.getModifiers());
        Class<?> type = field.getType();
        if(type.equals(boolean.class))
            return isVolatile ? (o,x) -> UNSAFE.putBooleanVolatile(o, offset, (boolean) x) : (o,x) -> UNSAFE.putBoolean(o, offset, (boolean) x);
        if(type.equals(byte.class))
            return isVolatile ? (o,x) -> UNSAFE.putByteVolatile(o, offset, (byte) x) : (o,x) -> UNSAFE.putByte(o, offset, (byte) x);
        if(type.equals(short.class))
            return isVolatile ? (o,x) -> UNSAFE.putShortVolatile(o, offset, (short) x) : (o,x) -> UNSAFE.putShort(o, offset, (short) x);
        if(type.equals(int.class))
            return isVolatile ? (o,x) -> UNSAFE.putIntVolatile(o, offset, (int) x) : (o,x) -> UNSAFE.putInt(o, offset, (int) x);
        if(type.equals(long.class))
            return isVolatile ? (o,x) -> UNSAFE.putLongVolatile(o, offset, (long) x) : (o,x) -> UNSAFE.putLong(o, offset, (long) x);
        if(type.equals(float.class))
            return isVolatile ? (o,x) -> UNSAFE.putFloatVolatile(o, offset, (float) x) : (o,x) -> UNSAFE.putFloat(o, offset, (float) x);
        if(type.equals(double.class))
            return isVolatile ? (o,x) -> UNSAFE.putDoubleVolatile(o, offset, (double) x) : (o,x) -> UNSAFE.putDouble(o, offset, (double) x);
        if(type.equals(char.class))
            return isVolatile ? (o,x) -> UNSAFE.putCharVolatile(o, offset, (char) x) : (o,x) -> UNSAFE.putChar(o, offset, (char) x);
        else return isVolatile ? (o,x) -> UNSAFE.putObjectVolatile(o, offset, x) : (o,x) -> UNSAFE.putObject(o, offset, x);
    }
}
