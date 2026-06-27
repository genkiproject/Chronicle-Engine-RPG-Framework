package com.chronicle.engine.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;

public record TextValue(String mode, String value) {
    public static final TextValue EMPTY = new TextValue("literal", "");

    public static TextValue parse(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return EMPTY;
        }
        if (element.isJsonPrimitive()) {
            return new TextValue("literal", element.getAsString());
        }
        JsonObject object = element.getAsJsonObject();
        String mode = JsonUtil.string(object, "mode", "literal");
        String value = JsonUtil.string(object, "value", "");
        return new TextValue(mode, value);
    }

    public Component component() {
        if ("translatable".equalsIgnoreCase(mode)) {
            return Component.translatable(value);
        }
        return Component.literal(value);
    }

    public String plain() {
        return value == null ? "" : value;
    }
}

