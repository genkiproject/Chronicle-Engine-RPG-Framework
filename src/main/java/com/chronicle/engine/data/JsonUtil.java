package com.chronicle.engine.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() ? element.getAsString() : fallback;
    }

    public static int integer(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() ? element.getAsInt() : fallback;
    }

    public static boolean bool(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() ? element.getAsBoolean() : fallback;
    }

    public static JsonObject object(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    public static List<JsonObject> objects(JsonObject object, String key) {
        List<JsonObject> result = new ArrayList<>();
        JsonElement element = object.get(key);
        if (element != null && element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonObject()) {
                    result.add(item.getAsJsonObject());
                }
            }
        }
        return result;
    }

    public static List<String> strings(JsonObject object, String key) {
        List<String> result = new ArrayList<>();
        JsonElement element = object.get(key);
        if (element != null && element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive()) {
                    result.add(item.getAsString());
                }
            }
        }
        return result;
    }

    public static List<JsonObject> objectValues(JsonObject object, String key) {
        List<JsonObject> result = new ArrayList<>();
        JsonElement element = object.get(key);
        if (element != null && element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    result.add(entry.getValue().getAsJsonObject());
                }
            }
        }
        return result;
    }

    public static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }
}

