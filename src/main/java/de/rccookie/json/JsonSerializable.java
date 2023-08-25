package de.rccookie.json;

public interface JsonSerializable {

    Object toJson();

    default JsonElement getJson() {
        return JsonElement.wrap(toJson());
    }
}
