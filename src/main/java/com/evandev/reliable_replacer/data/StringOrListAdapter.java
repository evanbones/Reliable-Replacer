package com.evandev.reliable_replacer.data;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class StringOrListAdapter implements JsonDeserializer<List<String>>, JsonSerializer<List<String>> {

    @Override
    public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<String> list = new ArrayList<>();
        if (json != null) {
            if (json.isJsonArray()) {
                for (JsonElement elem : json.getAsJsonArray()) {
                    if (elem.isJsonPrimitive()) {
                        list.add(elem.getAsString());
                    }
                }
            } else if (json.isJsonPrimitive()) {
                list.add(json.getAsString());
            }
        }
        return list;
    }

    @Override
    public JsonElement serialize(List<String> src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null || src.isEmpty()) return new JsonArray();
        if (src.size() == 1) {
            return new JsonPrimitive(src.getFirst());
        }
        JsonArray arr = new JsonArray();
        for (String s : src) {
            arr.add(s);
        }
        return arr;
    }
}
