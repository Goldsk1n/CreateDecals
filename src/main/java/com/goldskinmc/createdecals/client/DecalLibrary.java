package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Every decal the client knows, from all resource packs: {@code assets/<ns>/decals/<path>.json} defines decal
 * {@code <ns>:<path>}, whose sprite is {@code <ns>:decal/<path>} on the block atlas (textures/decal/, stitched by our
 * atlas source). A definition gives the size in tiles ({@code "size": [w, h]}, default 1x1) and a category for the
 * picker. A block asking for a decal not listed here draws nothing on that face.
 */
public final class DecalLibrary implements ResourceManagerReloadListener {
    public static final DecalLibrary INSTANCE = new DecalLibrary();
    private static final String FOLDER = "decals";

    public record Entry(ResourceLocation id, ResourceLocation sprite, int width, int height, String category) {
    }

    private volatile Map<ResourceLocation, Entry> entries = Map.of();
    private final Set<ResourceLocation> reportedMissing = ConcurrentHashMap.newKeySet();

    private DecalLibrary() {
    }

    @Nullable
    public Entry get(ResourceLocation id) {
        Entry entry = entries.get(id);
        if (entry == null && reportedMissing.add(id)) {
            CreateDecals.LOGGER.warn("Decal {} is not in any loaded resource pack; faces showing it stay plain", id);
        }
        return entry;
    }

    public Collection<Entry> all() {
        return entries.values();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<ResourceLocation, Entry> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> file : manager.listResources(FOLDER, path -> path.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation location = file.getKey();
            String path = location.getPath().substring(FOLDER.length() + 1, location.getPath().length() - ".json".length());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), path);
            try (Reader reader = file.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                int width = 1;
                int height = 1;
                if (json.has("size")) {
                    JsonArray size = GsonHelper.getAsJsonArray(json, "size");
                    width = Math.max(1, size.get(0).getAsInt());
                    height = Math.max(1, size.get(1).getAsInt());
                }
                JsonElement category = json.get("category");
                loaded.put(id, new Entry(id, ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "decal/" + path), width, height,
                        category != null ? category.getAsString() : "misc"));
            } catch (Exception e) {
                CreateDecals.LOGGER.warn("Skipping decal definition {}: {}", location, e.toString());
            }
        }
        entries = Map.copyOf(loaded);
        reportedMissing.clear();
        DecalQuads.clearCache();
        CreateDecals.LOGGER.info("Loaded {} decals", loaded.size());
    }
}
