package de.rccookie.json;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final ThreadLocal<Deque<JsonSerializer>> CURRENT_SERIALIZER = ThreadLocal.withInitial(ArrayDeque::new);

    private static final Function<JsonSerializable,?> JSON_SERIALIZABLE_SERIALIZER = JsonSerializable::toJson;
    private static final Function<?,JsonArray> ARRAY_SERIALIZER = arr -> {
        JsonArray json = new JsonArray();
        int length = Array.getLength(arr);
        for(int i=0; i<length; i++)
            json.add(Array.get(arr, i)); // add() calls serialize() internally
        return json;
    };


    private static final Function<JsonElement,?> NULL_SUPPLIER = $ -> null;


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
            ((Map<?,?>) map).forEach((k,v) -> serialized.put(serialize(k), serialize(v)));
            if(serialized.keySet().stream().allMatch(String.class::isInstance))
                return new JsonObject(serialized);
            return serialized.entrySet();
        });
        registerDeserializer(Collection.class, json -> json.asList(json.typeParameter()));
        registerDeserializer(List.class, json -> json.asList(json.typeParameter()));
        registerDeserializer(ArrayList.class, json -> json.asCollection(ArrayList::new, json.typeParameter()));
        registerDeserializer(LinkedList.class, json -> json.asCollection(LinkedList::new, json.typeParameter()));
        registerDeserializer(Vector.class, json -> json.asCollection(Vector::new, json.typeParameter()));
        registerDeserializer(Set.class, json -> json.asSet(json.typeParameter()));
        registerDeserializer(HashSet.class, json -> json.asCollection(HashSet::new, json.typeParameter()));
        registerDeserializer(LinkedHashSet.class, json -> json.asCollection(LinkedHashSet::new, json.typeParameter()));
        registerDeserializer(SortedSet.class, json -> json.asCollection(TreeSet::new, json.typeParameter()));
        registerDeserializer(NavigableSet.class, json -> json.asCollection(TreeSet::new, json.typeParameter()));
        registerDeserializer(TreeSet.class, json -> json.asCollection(TreeSet::new, json.typeParameter()));
        //noinspection unchecked,rawtypes
        registerDeserializer(EnumSet.class, json -> json.asCollection(() -> EnumSet.noneOf((Class) json.rawTypeParameter()), json.typeParameter()));
        registerDeserializer(Queue.class, json -> json.asCollection(ArrayDeque::new, json.typeParameter()));
        registerDeserializer(PriorityQueue.class, json -> json.asCollection(PriorityQueue::new, json.typeParameter()));
        registerDeserializer(Deque.class, json -> json.asCollection(ArrayDeque::new, json.typeParameter()));
        registerDeserializer(ArrayDeque.class, json -> json.asCollection(ArrayDeque::new, json.typeParameter()));
        registerDeserializer(Stack.class, json -> json.asCollection(Stack::new, json.typeParameter()));
        registerDeserializer(Map.class, json -> {
            Type[] types = json.typeParameters();
            return json.asMap(types[0], types[1]);
        });
        registerDeserializer(HashMap.class, json -> {
            Type[] types = json.typeParameters();
            return json.asCustomMap(HashMap::new, types[0], types[1]);
        });
        registerDeserializer(LinkedHashMap.class, json -> {
            Type[] types = json.typeParameters();
            return json.asCustomMap(LinkedHashMap::new, types[0], types[1]);
        });
        registerDeserializer(TreeMap.class, json -> {
            Type[] types = json.typeParameters();
            return json.asCustomMap(TreeMap::new, types[0], types[1]);
        });
        registerDeserializer(SortedMap.class, json -> {
            Type[] types = json.typeParameters();
            return json.asCustomMap(TreeMap::new, types[0], types[1]);
        });
        registerDeserializer(NavigableMap.class, json -> {
            Type[] types = json.typeParameters();
            return json.asCustomMap(TreeMap::new, types[0], types[1]);
        });
        registerDeserializer(IdentityHashMap.class, json -> {
            Type[] types = json.typeParameters();
            return json.asCustomMap(IdentityHashMap::new, types[0], types[1]);
        });
        registerDeserializer(EnumMap.class, json -> {
            Type[] types = json.typeParameters();
            //noinspection rawtypes,unchecked
            return json.asCustomMap(() -> new EnumMap(json.rawTypeParameters()[0]), types[0], types[1]);
        });
        registerDeserializer(Properties.class, json -> {
            Type[] types = json.typeParameters();
            return json.asCustomMap(Properties::new, types[0], types[1]);
        });
        //noinspection unchecked
        registerSerializer(Optional.class, o -> o.orElse(null));
        registerDeserializer(Optional.class, j -> Optional.ofNullable(j.as(j.typeParameter())));
        registerSerializer(OptionalInt.class, o -> o.isPresent() ? o.getAsInt() : null);
        registerDeserializer(OptionalInt.class, j -> j.isPresent() ? OptionalInt.of(j.asInt()) : OptionalInt.empty());
        registerSerializer(OptionalLong.class, (OptionalLong o) -> o.isPresent() ? o.getAsLong() : null);
        registerDeserializer(OptionalLong.class, j -> j.isPresent() ? OptionalLong.of(j.asLong()) : OptionalLong.empty());
        registerSerializer(OptionalDouble.class, (OptionalDouble o) -> o.isPresent() ? o.getAsDouble() : null);
        registerDeserializer(OptionalDouble.class, j -> j.isPresent() ? OptionalDouble.of(j.asInt()) : OptionalDouble.empty());
        registerSerializer(Stream.class, s -> {
            JsonArray json = new JsonArray();
            ((Stream<?>) s).forEach(json::add);
            return json;
        });
        registerDeserializer(Stream.class, json -> {
            Type contentType = json.typeParameter();
            return json.stream().map(x -> x.as(contentType));
        });
        registerSerializer(Iterable.class, i -> {
            JsonArray json = new JsonArray();
            for(Object o : i)
                json.add(o);
            return json;
        });
        registerDeserializer(Iterable.class, json -> {
            Type contentType = json.typeParameter();
            return () -> new Iterator<>() {
                final Iterator<JsonElement> it = json.iterator();
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }
                @Override
                public Object next() {
                    return it.next().as(contentType);
                }
            };
        });
        registerSerializer(InetAddress.class, InetAddress::getHostAddress);
        registerDeserializer(InetAddress.class, json -> {
            try {
                return InetAddress.getByName(json.asString());
            } catch(UnknownHostException e) {
                throw new RuntimeException(e);
            }
        });
        registerDeserializer(Inet4Address.class, json -> {
            InetAddress addr = json.as(InetAddress.class);
            if(!(addr instanceof Inet4Address))
                throw new IllegalArgumentException("Not a IPv4 address: "+json.asString());
            return (Inet4Address) addr;
        });
        registerDeserializer(Inet6Address.class, json -> {
            InetAddress addr = json.as(InetAddress.class);
            if(!(addr instanceof Inet6Address))
                throw new IllegalArgumentException("Not a IPv6 address: "+json.asString());
            return (Inet6Address) addr;
        });
        registerSerializer(InetSocketAddress.class, addr -> new JsonObject("address", addr.getAddress(), "port", addr.getPort()));
        registerDeserializer(InetSocketAddress.class, json -> new InetSocketAddress(json.get("address").as(InetAddress.class), json.getInt("port")));


        try {
            //noinspection unchecked
            Class<? super LinkedHashMap<?,?>> sequencedMapCls = (Class<? super LinkedHashMap<?,?>>) Class.forName("java.util.SequencedMap");
            //noinspection unchecked
            Class<? super LinkedHashSet<?>> sequencedSetCls = (Class<? super LinkedHashSet<?>>) Class.forName("java.util.SequencedSet");
            //noinspection unchecked
            Class<? super List<?>> sequencedCollectionCls = (Class<? super List<?>>) Class.forName("java.util.SequencedCollection");

            registerDeserializer(sequencedMapCls, json -> {
                Type[] types = json.typeParameters();
                return json.asCustomMap(LinkedHashMap::new, types[0], types[1]);
            });
            registerDeserializer(sequencedSetCls, json -> json.asCollection(LinkedHashSet::new, json.typeParameter()));
            registerDeserializer(sequencedCollectionCls, json -> json.asList(json.typeParameter()));
        } catch(ClassNotFoundException ignored) {
            // < Java 21
        }


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
        registerDeserializer(URL.class, j -> {try{ return URI.create(j.asString()).toURL(); }catch(MalformedURLException e){ throw new RuntimeException(e); }});
        registerSerializer(URI.class, URI::toString);
        registerDeserializer(URI.class, j -> URI.create(j.asString()));
        registerSerializer(UUID.class, Object::toString);
        registerDeserializer(UUID.class, j -> UUID.fromString(j.asString()));
        registerSerializer(Date.class, Date::getTime);
        registerDeserializer(Date.class, j -> new Date(j.asLong()));
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

    static <T> T usingSerializer(JsonSerializer serializer, Supplier<? extends T> code) {
        CURRENT_SERIALIZER.get().push(Objects.requireNonNull(serializer, "serializer"));
        try {
            return code.get();
        } finally {
            CURRENT_SERIALIZER.get().pop();
        }
    }

    static Object serialize(Object obj) {
        if(isJsonTypeInstance(obj) && !(obj instanceof JsonStructure))
            return obj;

        Deque<JsonSerializer> currentSerializer = CURRENT_SERIALIZER.get();
        JsonSerializer serializer = currentSerializer.isEmpty() ? JsonSerializer.DEFAULT : currentSerializer.getFirst();
        Object res = serializer.serialize(obj);
        if(serializer == JsonSerializer.DEFAULT)
            return res;
        return usingSerializer(JsonSerializer.DEFAULT, () -> serialize(res));
    }

    @SuppressWarnings("unchecked")
    static Object doSerialize(Object obj) {
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
    static Object deserialize(Type type, JsonElement data) {
        if(data.isEmpty()) {
            Class<?> cls = data.resolveRawType(type);
            return cls.isArray() ? Array.newInstance(cls.getComponentType(), 0) : null;
        }
        data = data.pushCurrentType(type);
        try {
            return getDeserializer(type).apply(data);
        } catch(IllegalJsonDeserializerException | JsonDeserializationException e) {
            throw e;
        } catch(RuntimeException e) {
            throw new GeneralJsonDeserializationException(data, type, e);
        } finally {
            data.popCurrentType();
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

        // Try to find serializer of implemented interface. If multiple implemented interfaces have,
        // serializers registered, try to find the most specific interface
        Class<?> supportedInterface = getImplementedInterfaces(type).stream()
                .filter(INTERFACE_SERIALIZERS::containsKey)
                .reduce((a,b) -> a.isAssignableFrom(b) ? b : a)
                .orElse(null);
        if(supportedInterface != null)
            return INTERFACE_SERIALIZERS.get(supportedInterface);

        // Check for enum AFTER searching for custom serializer
        if(Enum.class.isAssignableFrom(type)) {
            while(!type.isEnum())
                type = type.getSuperclass();
            //noinspection rawtypes,unchecked
            return generateEnumSerializer((Class) type);
        }

        // Check for record also after searching for custom serializer
        if(isRecord(type))
            return generateRecordSerializer(type);

        if(!type.isInterface() && !Modifier.isAbstract(type.getModifiers()))
            return generateUnsafeSerializer(type);

        throw new IllegalJsonTypeException(type);
    }

    private static <E extends Enum<E>> Function<E,String> generateEnumSerializer(Class<E> type) {
        EnumMap<E, String> names = new EnumMap<>(type);
        for(E e : type.getEnumConstants())
            names.put(e, jsonName(e, type));
        return names::get;
    }

    private static Function<?,?> generateRecordSerializer(Class<?> type) {
        try {
            List<BiConsumer<Object, JsonObject>> readers = new ArrayList<>();
            for(Field f : type.getDeclaredFields()) {
                if(Modifier.isStatic(f.getModifiers())) continue;

                Method accessor = type.getMethod(f.getName());
                accessor.setAccessible(true);
                String jsonName = jsonName(f);

                readers.add((obj,json) -> { try {
                    json.put(jsonName, serialize(accessor.invoke(obj)));
                } catch(IllegalAccessException e) {
                    throw new AssertionError(e);
                } catch(InvocationTargetException e) {
                    throw rethrow(e);
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
        String[] names = new String[fields.length];
        for(int i=0; i<fields.length; i++) {
            getters[i] = getUnsafeGetter(fields[i]);
            names[i] = jsonName(fields[i]);
        }

        return obj -> {
            JsonObject json = new JsonObject();
            for(int i=0; i<getters.length; i++)
                json.put(names[i], serialize(getters[i].apply(obj)));
            return json;
        };
    }



    @SuppressWarnings({"unchecked", "DuplicatedCode", "rawtypes"})
    private static Function<JsonElement,?> getDeserializer(Type type) throws IllegalJsonDeserializerException {
        if(type == Object.class)
            throw new IllegalJsonDeserializerException("Cannot deserialize to java.lang.Object");

        if(type instanceof TypeVariable<?>)
            return json -> json.as(json.getActualType((TypeVariable<?>) type));

        if(type instanceof WildcardType) {
            Type[] lowerBounds = ((WildcardType) type).getLowerBounds();
            if(lowerBounds.length != 0)
                return json -> json.as(lowerBounds[0]);
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            return json -> json.as(upperBounds[0]);
        }

        if(type instanceof GenericArrayType) {
            Type genericContentType = ((GenericArrayType) type).getGenericComponentType();
            Type deepGenericContentType = genericContentType;
            int depth = 1;
            while(deepGenericContentType instanceof GenericArrayType) {
                deepGenericContentType = ((GenericArrayType) deepGenericContentType).getGenericComponentType();
                depth++;
            }
            Type _deepGenericContentType = deepGenericContentType;
            int _depth = depth;
            return json -> {

                Type rawContentType = _deepGenericContentType;
                while(rawContentType instanceof TypeVariable<?>)
                    rawContentType = json.getActualType((TypeVariable<?>) rawContentType);
                if(rawContentType instanceof ParameterizedType)
                    rawContentType = ((ParameterizedType) rawContentType).getRawType();
                for(int i=1; i<_depth; i++)
                    rawContentType = Array.newInstance((Class<?>) rawContentType, 0).getClass();

                Object arr = Array.newInstance((Class<?>) rawContentType, json.size());
                for(int i=0; i<json.size(); i++)
                    Array.set(arr, i, json.get(i).as(genericContentType));
                return arr;
            };
        }

        if(type instanceof ParameterizedType) {
            ParameterizedType genericType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) genericType.getRawType();
            TypeVariable<?>[] genericParams = rawType.getTypeParameters();
            return json -> {
                json = json.pushGenericContext(genericParams, genericType.getActualTypeArguments());
                Object obj = json.as(rawType);
                json.popGenericContext(genericParams);
                return obj;
            };
        }

        Class<?> cls = (Class<?>) type;

        // Deserialize to array component-wise
        if(cls.isArray()) {
            Class<?> contentType = cls.getComponentType();
            return json -> {
                Object arr = Array.newInstance(cls.getComponentType(), json.size());
                for(int i=0; i<json.size(); i++)
                    Array.set(arr, i, json.get(i).as(contentType));
                return arr;
            };
        }

        // Initialize class giving a chance to register a deserializer
        loadType(cls);

        // Check for custom or cached deserializer
        Function<JsonElement,?> deserializer = DESERIALIZERS.get(Objects.requireNonNull(type));
        if(deserializer != null) return deserializer;

        // Check for enum AFTER searching for custom deserializer
        if(cls.isEnum())
            return generateEnumDeserializer((Class) type);

        try {
            Constructor<?> ctor = cls.getDeclaredConstructor(/* paramType: */ JsonElement.class);
            ctor.setAccessible(true);
            deserializer = json -> {
                try { return ctor.newInstance(json); }
                catch(Exception e) { throw rethrow(e); }
            };
            DESERIALIZERS.put(cls, deserializer);
            return deserializer;
        } catch(NoSuchMethodException ignored) { }

        if(isRecord(cls)) {
            deserializer = getRecordDeserializer(cls);
            DESERIALIZERS.put(cls, deserializer);
            return deserializer;
        }
        if(!cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
            deserializer = getUnsafeDeserializer(cls);
            DESERIALIZERS.put(cls, deserializer);
            return deserializer;
        }

        throw new IllegalJsonDeserializerException("The type " + cls.getSimpleName() + " is not marked from json deserialization");
    }

    private static Function<JsonElement,?> getRecordDeserializer(Class<?> type) {
        Field[] fields = Arrays.stream(type.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).toArray(Field[]::new);
        Class<?>[] rawTypes = new Class[fields.length];
        Type[] types = new Type[fields.length];
        String[] names = new String[fields.length];
        for(int i=0; i<fields.length; i++) {
            rawTypes[i] = fields[i].getType();
            types[i] = fields[i].getGenericType();
            names[i] = jsonName(fields[i]);
        }

        try {
            Constructor<?> ctor = type.getDeclaredConstructor(rawTypes);
            ctor.setAccessible(true);
            Function<JsonElement,?>[] defaults = getRecordDefaults(fields, ctor);

            if(types.length == 1) return json -> {
                try {
                    Object param;
                    try {
                        JsonElement jParam = json.get(names[0]);
                        if(jParam.isPresent())
                            param = jParam.as(types[0]);
                        else param = defaults[0].apply(json);
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
                    throw rethrow(e);
                } catch(InstantiationException | IllegalAccessException e) {
                    throw new AssertionError(e); // Access: Should have failed during setAccessible()
                }
            };

            return json -> {
                try {
                    Object[] args = new Object[types.length];
                    for(int i=0; i<args.length; i++) {
                        JsonElement jParam = json.get(names[i]);
                        if(jParam.isPresent())
                            args[i] = jParam.as(types[i]);
                        else args[i] = defaults[i].apply(json);
                    }
                    return ctor.newInstance(args);
                } catch(InvocationTargetException e) {
                    throw rethrow(e);
                } catch(InstantiationException | IllegalAccessException e) {
                    throw new AssertionError(e); // Access: Should have failed during setAccessible()
                }
            };
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Function<JsonElement,?> getUnsafeDeserializer(Class<?> type) {
        Field[] fields = Arrays.stream(type.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers())).toArray(Field[]::new);
        Type[] types = new Type[fields.length];
        BiConsumer<Object, Object>[] setters = new BiConsumer[fields.length];
        String[] names = new String[fields.length];
        for(int i=0; i<fields.length; i++) {
            types[i] = fields[i].getGenericType();
            setters[i] = getUnsafeSetter(fields[i]);
            names[i] = jsonName(fields[i]);
        }
        Function<JsonElement,?>[] defaults = getDefaults(fields);

        return json -> {
            try {
                Object obj = UNSAFE.allocateInstance(type);
                for(int i=0; i<fields.length; i++) {
                    JsonElement jField = json.get(names[i]);
                    Object val;
                    if(jField.isPresent())
                        val = jField.as(types[i]);
                    else val = defaults[i].apply(json);
                    setters[i].accept(obj, val);
                }
                return obj;
            } catch(InstantiationException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Function<JsonElement,?> [] getRecordDefaults(Field[] fields, Constructor<?> ctor) {
        Function<JsonElement,?>[] defaults = new Function[fields.length];
        Annotation[][] annotations = ctor.getParameterAnnotations();
        for(int i=0; i<defaults.length; i++)
            defaults[i] = getDefault(annotations[i], fields[i].getType(), fields[i].getGenericType(), jsonName(fields[i]), fields[i].getDeclaringClass() + "." + fields[i].getName());
        return defaults;
    }

    @SuppressWarnings("unchecked")
    private static Function<JsonElement,?> [] getDefaults(Field[] fields) {
        return Arrays.stream(fields)
                .map(f -> getDefault(f.getAnnotations(), f.getType(), f.getGenericType(), jsonName(f), f.getDeclaringClass()+"."+f.getName()))
                .toArray(Function[]::new);
    }

    private static Function<JsonElement,?> getDefault(Annotation[] annotations, Class<?> rawType, Type genericType, String jsonFieldName, String name) {
        try {
            Function<JsonElement,?> supplier = null;
            boolean required = false;
            boolean noDefault = false;
            for(Annotation a : annotations) {
                if(a instanceof Default)
                    supplier = parseDefault(name, (Default) a, genericType);
                else {
                    required |= a instanceof Required || a instanceof NotNull;
                    noDefault |= a instanceof Required;
                }
            }
            if(supplier != null) {
                if(noDefault)
                    throw new IllegalJsonDeserializerException(name+": Cannot be required and have a default value");
                return supplier;
            }
            if(!required && !rawType.isPrimitive())
                return NULL_SUPPLIER;
            return json -> { throw new MissingFieldException(genericType, Json.appendToPath(json.path(), jsonFieldName)); };
        } catch(IllegalDefaultValueException e) {
            throw new IllegalDefaultValueException(name+": "+e.getMessage(), e);
        }
    }


    private static <E extends Enum<E>> Function<JsonElement, E> generateEnumDeserializer(Class<E> enumType) {
        return new Function<>() {
            final E[] values = enumType.getEnumConstants();
            final Map<String, E> constantsByName = new HashMap<>();
            final Map<String, E> constantsByLowerName = new HashMap<>();
            final Map<String, List<E>> ambiguousLowerNames = new HashMap<>();
            {
                for(E e : enumType.getEnumConstants()) {
                    String name = jsonName(e, enumType);
                    constantsByName.put(name, e);
                    String lower = name.toLowerCase();
                    if(ambiguousLowerNames.containsKey(lower)) {
                        ambiguousLowerNames.get(lower).add(e);
                        constantsByLowerName.remove(lower);
                    }
                    else {
                        List<E> ambiguous = new ArrayList<>();
                        ambiguous.add(e);
                        ambiguousLowerNames.put(lower, ambiguous);
                        if(constantsByLowerName.put(lower, e) != null)
                            constantsByLowerName.remove(lower);
                    }
                }
                ambiguousLowerNames.values().removeIf(ns -> ns.size() <= 1);
            }

            @Override
            public E apply(JsonElement json) {
                if(json.isInteger()) {
                    int index = json.asInt();
                    if(index < 0 || index >= values.length)
                        throw new IllegalEnumConstantException("Index "+index+" in '"+json.path()+"' out of bounds for allowed "+enumType.getSimpleName()+" constants");
                    return values[json.asInt()];
                }
                String name = json.asString();
                E e = constantsByName.get(name);
                if(e != null) return e;

                String lower = name.toLowerCase();
                e = constantsByLowerName.get(lower);
                if(e != null) return e;

                if(ambiguousLowerNames.containsKey(lower))
                    throw new IllegalEnumConstantException("'"+name+"' is ambiguous between "+enumType.getSimpleName()+" constants "+join(ambiguousLowerNames.get(lower))+" for '"+json.path()+"'");
                throw new IllegalEnumConstantException("'"+name+"' is an invalid "+enumType.getSimpleName()+" for '"+json.path()+"', allowed values are "+join(Arrays.asList(values)));
            }

            private String join(List<E> list) {
                return list.subList(0, list.size() - 1).stream().map(e -> "'"+e.name()+"'").collect(Collectors.joining(", ")) + " and '" + list.get(list.size() - 1)+"'";
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
        Class<?> superclass = type.getSuperclass();
        return superclass != null && superclass.getName().equals("java.lang.Record");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Function<JsonElement,?> parseDefault(String context, Default d, Type type) {
        String value = d.value();
        if(type instanceof Class<?> && ((Class<?>) type).isEnum()) {
            Object val = !d.string() && value.startsWith("\"") ? Json.parse(value).as(type) : Enum.valueOf((Class) type, value);
            return $ -> val;
        }
        if(type == String.class) {
            String str = d.string() ? value : Json.parse(value).asString();
            return $ -> str;
        }

        if(!d.string() && value.equals("null")) return NULL_SUPPLIER;
        try {
            JsonElement json = d.string() ? JsonElement.wrap(value) : Json.parse(value);
            return contextJson -> {
                try {
                    return json.withContextOf(contextJson).as(type);
                } catch(Exception e) {
                    throw new IllegalJsonDeserializerException(context+": failed to deserialize default value", e);
                }
            };
        } catch(JsonParseException e) {
            throw new IllegalJsonDeserializerException(context+": default value is not proper json: "+e.getMessage(), e);
        }
    }

    private static String jsonName(Field field) {
        JsonName nameAnnotation = field.getAnnotation(JsonName.class);
        return nameAnnotation != null ? nameAnnotation.value() : field.getName();
    }

    private static <E extends Enum<E>> String jsonName(E enumInstance, Class<E> enumType) {
        try {
            return jsonName(enumType.getDeclaredField(enumInstance.name()));
        } catch(NoSuchFieldException e) {
            throw new AssertionError(e);
        }
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
        else
            return isVolatile ? o -> UNSAFE.getObjectVolatile(o, offset) : o -> UNSAFE.getObject(o, offset);
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
        else
            return isVolatile ? (o,x) -> UNSAFE.putObjectVolatile(o, offset, x) : (o,x) -> UNSAFE.putObject(o, offset, x);
    }

    private static RuntimeException rethrow(Throwable e) {
        while(e instanceof InvocationTargetException)
            e = e.getCause();
        if(e instanceof RuntimeException)
            return (RuntimeException) e;
        if(e instanceof Error)
            throw (Error) e;
        return new RuntimeException(e);
    }
}
