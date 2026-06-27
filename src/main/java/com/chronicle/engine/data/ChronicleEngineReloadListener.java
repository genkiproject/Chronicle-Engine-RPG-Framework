package com.chronicle.engine.data;

import com.chronicle.engine.ChronicleEngine;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class ChronicleEngineReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    public ChronicleEngineReloadListener() {
        super(GSON, "chronicle");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        ChronicleEngine.LOGGER.info("Loading {} ChronicleEngine json resources", objects.size());
        ChronicleEngineData.apply(objects);
    }
}

