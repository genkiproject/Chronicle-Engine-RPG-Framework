package com.chronicle.engine;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChronicleEngine {
    public static final String MOD_ID = "chronicle_engine";
    public static final Logger LOGGER = LoggerFactory.getLogger("Chronicle Engine: RPG Framework");

    private ChronicleEngine() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}

