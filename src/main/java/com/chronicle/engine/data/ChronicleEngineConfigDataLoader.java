package com.chronicle.engine.data;

import com.chronicle.engine.ChronicleEngine;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChronicleEngineConfigDataLoader {
    public static final String PACK_NAME = "chronicle_pack";
    private static final List<String> DATA_DIRS = List.of("dialogues", "quests", "npc", "trades", "shops", "wallet");
    private static final Gson GSON = new Gson();

    private ChronicleEngineConfigDataLoader() {
    }

    public static Map<ResourceLocation, JsonElement> load() {
        Map<ResourceLocation, JsonElement> result = new LinkedHashMap<>();
        Path root = root();
        ensureFolders(root);

        for (String folder : DATA_DIRS) {
            Path directory = root.resolve(folder);
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (var files = Files.walk(directory)) {
                files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                        .forEach(path -> loadFile(result, root, folder, path));
            } catch (Exception exception) {
                ChronicleEngine.LOGGER.error("Failed to scan ChronicleEngine config folder {}: {}", directory, exception.getMessage(), exception);
            }
        }
        if (!result.isEmpty()) {
            ChronicleEngine.LOGGER.info("Loaded {} ChronicleEngine config json files from {}", result.size(), root.toAbsolutePath());
        }
        return result;
    }

    public static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve(ChronicleEngine.MOD_ID).resolve(PACK_NAME);
    }

    private static void ensureFolders(Path root) {
        for (String folder : DATA_DIRS) {
            try {
                Files.createDirectories(root.resolve(folder));
            } catch (Exception exception) {
                ChronicleEngine.LOGGER.error("Failed to create ChronicleEngine config folder {}: {}", root.resolve(folder), exception.getMessage(), exception);
            }
        }
    }

    private static void loadFile(Map<ResourceLocation, JsonElement> result, Path root, String folder, Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            Path relative = root.resolve(folder).relativize(path);
            String name = relative.toString().replace('\\', '/');
            if (name.endsWith(".json")) {
                name = name.substring(0, name.length() - ".json".length());
            }
            result.put(new ResourceLocation(PACK_NAME, folder + "/" + name), json);
        } catch (Exception exception) {
            ChronicleEngine.LOGGER.error("Failed to load ChronicleEngine config file {}: {}", path, exception.getMessage(), exception);
        }
    }
}


