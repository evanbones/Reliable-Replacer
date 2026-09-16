package com.evandev.reliable_replacer.data;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

public class StringOrSetAdapter implements JsonDeserializer<Set<String>>, JsonSerializer<Set<String>> {

    @Override
    public Set<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Set<String> set = new LinkedHashSet<>();
        if (json != null) {
            if (json.isJsonArray()) {
                for (JsonElement elem : json.getAsJsonArray()) {
                    if (elem.isJsonPrimitive()) {
                        set.add(elem.getAsString());
                    }
                }
            } else if (json.isJsonPrimitive()) {
                set.add(json.getAsString());
            }
        }
        return set;
    }

    @Override
    public JsonElement serialize(Set<String> src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null || src.isEmpty()) return new JsonArray();
        if (src.size() == 1) {
            return new JsonPrimitive(src.iterator().next());
        }
        JsonArray arr = new JsonArray();
        for (String s : src) {
            arr.add(s);
        }
        return arr;
    }
}
