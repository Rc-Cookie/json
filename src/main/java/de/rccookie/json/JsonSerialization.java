package de.rccookie.json;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

final class JsonSerialization {

    private JsonSerialization() { }


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
    private static final Map<Class<?>, Function<JsonElement,?>> DESERIALIZERS = new HashMap<>();
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

        registerSerializer(Object.class, $ -> new JsonObject());
        registerDeserializer(Object.class, JsonElement::get);

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

        registerSerializer(File.class, File::getPath);
        registerDeserializer(File.class, j -> new File(j.asString()));
        registerSerializer(Path.class, Object::toString);
        registerDeserializer(Path.class, j -> Path.of(j.asString()));
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
     */
    @SuppressWarnings("unchecked")
    static <T> T deserialize(Class<T> type, JsonElement data) {
        if(data.isEmpty())
            return type.isArray() ? (T) Array.newInstance(type.getComponentType(), 0) : null;
        try {
            return getDeserializer(type).apply(data);
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

        throw new IllegalJsonTypeException(type);
    }

    private static Function<?,?> generateRecordSerializer(Class<?> type) {
        try {
            List<BiConsumer<Object, JsonObject>> readers = new ArrayList<>();
            for(Field f : type.getDeclaredFields()) {
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



    @SuppressWarnings({"unchecked", "DuplicatedCode", "rawtypes"})
    private static <T> Function<JsonElement, T> getDeserializer(Class<T> type) {
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
            } catch (NoSuchMethodException e) {
                if(isRecord(type)) {
                    deserializer = getRecordDeserializer(type);
                    DESERIALIZERS.put(type, deserializer);
                    return deserializer;
                }
                throw new IllegalArgumentException("The type " + type + " is not marked from json deserialization");
            }
        }

        Supplier<?>[] defaults = getDefaults(ctor);
        JsonCtor props = ctor.getAnnotation(JsonCtor.class);
        Class<?>[] paramTypes = ctor.getParameterTypes();
        Type packaging = props.type();

//            ctor.getParameters()[0].getName();

        // Json object packaged
        if(packaging == Type.OBJECT || (packaging == Type.AUTO && props.value().length != 0)) {
            String[] keys = props.value();
            if(keys.length != paramTypes.length)
                throw new IllegalArgumentException("The annotated constructor of "+type+" has the wrong number of parameters");
            deserializer = j -> {
                Object[] params = new Object[keys.length];
                for(int i=0; i<params.length; i++) {
                    params[i] = j.get(keys[i]).orGet(paramTypes[i], defaults[i]);
                    if(paramTypes[i].isPrimitive() && params[i] == null)
                        throw new NullPointerException("Missing "+paramTypes[i]+" value \""+keys[i]+"\" for json deserialization");
                }
                try { return ctor.newInstance(params); }
                catch(Exception e) { throw new RuntimeException(e); }
            };
        }
        // Json array packaged
        else if(packaging == Type.ARRAY || (packaging == Type.AUTO && props.indices().length != 0)) {
            int[] indices = props.indices();
            if(indices.length == 0) {
                // No indices specified -> use index of parameter
                deserializer = j -> {
                    Object[] params = new Object[paramTypes.length];
                    for(int i=0; i<params.length; i++) {
                        params[i] = j.get(i).orGet(paramTypes[i], defaults[i]);
                        if(paramTypes[i].isPrimitive() && params[i] == null)
                            throw new NullPointerException("Missing "+paramTypes[i]+" value ["+i+"] for json deserialization");
                    }
                    try { return ctor.newInstance(params); }
                    catch (Exception e) { throw new RuntimeException(e); }
                };
            }
            else {
                if(indices.length != paramTypes.length)
                    throw new IllegalArgumentException("The annotated constructor of " + type + " has the wrong number of parameters");
                deserializer = j -> {
                    Object[] params = new Object[indices.length];
                    for(int i=0; i<params.length; i++) {
                        params[i] = j.get(indices[i]).orGet(paramTypes[i], defaults[i]);
                        if(paramTypes[i].isPrimitive() && params[i] == null)
                            throw new NullPointerException("Missing "+paramTypes[i]+" value ["+indices[i]+"] for json deserialization");
                    }
                    try { return ctor.newInstance(params); }
                    catch (Exception e) { throw new RuntimeException(e); }
                };
            }
        }
        // Not packaged
        else {
            if(paramTypes.length != 1)
                throw new IllegalArgumentException("The annotated json of "+type+" constructor may have exactly one parameter");
            deserializer = j -> {
                try { return ctor.newInstance(deserialize(paramTypes[0], j)); }
                catch(Exception e) { throw new RuntimeException(e); }
            };
        }
        CONCRETE_SERIALIZERS.put(type, deserializer);
        return deserializer;
    }

    private static <T> Function<JsonElement,T> getRecordDeserializer(Class<T> type) {
        Field[] fields = type.getDeclaredFields();
        Class<?>[] types = new Class[fields.length];
        for(int i=0; i<fields.length; i++)
            types[i] = fields[i].getType();
        try {
            Constructor<T> ctor = type.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            Supplier<?>[] defaults = getDefaults(ctor);
            return json -> {
                try {
                    // Single element records are unpacked
                    if(types.length == 1)
                        return ctor.newInstance(json.as(types[0]));

                    Object[] args = new Object[types.length];
                    for(int i=0; i<args.length; i++) {
                        args[i] = json.get(fields[i].getName()).orGet(types[i], defaults[i]);
                        if(types[i].isPrimitive() && args[i] == null)
                            throw new NullPointerException("Missing "+args[i]+" value \""+fields[i].getName()+"\" for json deserialization");
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

    private static Supplier<?>[] getDefaults(Constructor<?> ctor) {
        Class<?>[] params = ctor.getParameterTypes();
        Annotation[][] annotations = ctor.getParameterAnnotations();
        Supplier<?>[] defaults = new Supplier<?>[params.length];
        paramLoop: for(int i=0; i<params.length; i++) try {
            for(Annotation a : annotations[i]) {
                if(a instanceof Default) {
                    defaults[i] = parseDefault((Default) a, params[i]);
                    continue paramLoop;
                }
            }
            defaults[i] = NULL_SUPPLIER;
        } catch(IllegalDefaultValueException e) {
            throw new IllegalDefaultValueException(ctor+": parameter "+(i+1)+": "+e.getMessage(), e);
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

    private static Supplier<?> parseDefault(Default d, Class<?> type) {
        String[] values = d.value();
        if(type.isArray()) {
            Supplier<?>[] suppliers = new Supplier<?>[values.length];
            for(int i=0; i<values.length; i++)
                suppliers[i] = parseDefault(values[i], type.getComponentType(), d);
            return () -> {
                Object arr = Array.newInstance(type.getComponentType(), suppliers.length);
                for(int i=0; i<suppliers.length; i++)
                    Array.set(arr, i, suppliers[i].get());
                return arr;
            };
        }
        if(type == Collection.class || type == Set.class || type == List.class) {
            Class<?> contentType = getValueType(d);
            List<Supplier<?>> suppliers = new ArrayList<>();
            for(String value : values)
                suppliers.add(parseDefault(value, contentType, d));
            if(type == Set.class)
                return () -> suppliers.stream().map(Supplier::get).collect(Collectors.toSet());
            else return () -> suppliers.stream().map(Supplier::get).collect(Collectors.toList());
        }
        if(values.length != 1)
            throw new IllegalDefaultValueException("Parameter has non-array or collection type "+type.getSimpleName()+"; default value should not be array");
        return parseDefault(values[0], type, d);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Supplier<?> parseDefault(String value, Class<?> type, Default d) {
        if(value.equals("null")) return NULL_SUPPLIER;
        if(!value.startsWith("\"")) {
            if(type == String.class)
                return () -> value;
            if(type.isEnum()) {
                Object val = Enum.valueOf((Class) type, value);
                return () -> val;
            }
        }
        JsonElement json = Json.parse(value);
        if(type == List.class || type == Collection.class)
            return () -> json.asList(getValueType(d));
        if(type == Set.class)
            return () -> json.asSet(getValueType(d));
        if(type == Map.class)
            return () -> json.asMap(d.keyType(), getValueType(d));
        return () -> json.as(type);
    }

    private static Class<?> getValueType(Default d) {
        if(d.valueType() == void.class)
            throw new IllegalDefaultValueException("The 'valueType' parameter is required for the default value of a Collection or Map");
        return d.valueType();
    }
}
