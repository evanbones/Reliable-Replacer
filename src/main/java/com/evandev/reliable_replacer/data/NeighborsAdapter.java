package com.evandev.reliable_replacer.data;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.*;

public class NeighborsAdapter implements JsonDeserializer<Map<String, List<String>>>, JsonSerializer<Map<String, List<String>>> {

    @Override
    public Map<String, List<String>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map<String, List<String>> result = new HashMap<>();
        if (json != null && json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String direction = entry.getKey();
                JsonElement val = entry.getValue();
                List<String> list = new ArrayList<>();
                if (val.isJsonArray()) {
                    for (JsonElement elem : val.getAsJsonArray()) {
                        if (elem.isJsonPrimitive()) {
                            list.add(elem.getAsString());
                        }
                    }
                } else if (val.isJsonPrimitive()) {
                    list.add(val.getAsString());
                }
                result.put(direction, list);
            }
        }
        return result;
    }

    @Override
    public JsonElement serialize(Map<String, List<String>> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        if (src != null) {
            for (Map.Entry<String, List<String>> entry : src.entrySet()) {
                List<String> list = entry.getValue();
                if (list != null && !list.isEmpty()) {
                    if (list.size() == 1) {
                        obj.addProperty(entry.getKey(), list.getFirst());
                    } else {
                        JsonArray arr = new JsonArray();
                        for (String s : list) {
                            arr.add(s);
                        }
                        obj.add(entry.getKey(), arr);
                    }
                }
            }
        }
        return obj;
    }
}
