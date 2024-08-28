package de.rccookie.json.linked;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import de.rccookie.json.Json;
import de.rccookie.json.JsonArray;
import de.rccookie.json.JsonObject;
import de.rccookie.json.JsonSerializer;
import de.rccookie.json.JsonStructure;

/**
 * A json serializer that, in combination with {@link LinkedDeserializer}, retains
 * relationships between objects where multiple instances share a reference to the
 * same instance of an object, in other words, it maintains the <code>==</code>
 * relationship between serialized objects. Note that cyclic references between
 * objects are still not supported and may lead to {@link StackOverflowError}s.
 */
public class LinkedSerializer extends JsonSerializer {

    @Override
    public Object serialize(Object data) {
        Instance instance = new Instance();
        Json.serialize(data, instance);
        return instance.result();
    }

    private static final class Instance extends JsonSerializer {

        private final Map<Object, Object> instanceRefs = new IdentityHashMap<>();
        private final List<Object> serialized = new ArrayList<>();

        @Override
        public Object serialize(Object data) {
            if(data == null || data instanceof Ref) return data;

            Object ref = instanceRefs.get(data);
            if(ref == null) {

                Object serial;
                if(data instanceof JsonObject)
                    serial = data;
                else if(data instanceof JsonArray)
                    serial = data;
                else serial = super.serialize(data);

                if(serial instanceof Ref || !(serial instanceof JsonStructure))
                    ref = serial;
                else {
                    ref = new Ref(instanceRefs.size());
                    if(instanceRefs.put(data, ref) != null)
                        throw new AssertionError();
                    serialized.add(serial);
                }
            }
            return ref;
        }

        Object result() {
            return serialized;
        }

        private static final class Ref extends JsonArray {
            Ref(int id) {
                super(id);
            }
        }
    }
}
