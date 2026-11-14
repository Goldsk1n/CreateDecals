package com.goldskinmc.createdecals.decal;

import com.goldskinmc.createdecals.CreateDecals;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.jetbrains.annotations.Nullable;

/**
 * Decal ids. A decal is data, not code: its definition is {@code assets/<namespace>/decals/<path>.json} and its sprite
 * {@code assets/<namespace>/textures/decal/<path>.png}, read by the client ({@code client/DecalLibrary}). The server
 * only stores and syncs ids, so resource packs can add decals without the server knowing them.
 */
public final class Decals {
    private static List<ResourceLocation> builtIn;

    private Decals() {
    }

    /** One of our own decals by its path, e.g. {@code digit_7}. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateDecals.MOD_ID, path);
    }

    /**
     * A stored id; bare paths (as the prototype saved them) mean our own decals. Null when it is not an id at all (damaged
     * or hand-edited data): that face then shows no decal, rather than some other one.
     */
    @Nullable
    public static ResourceLocation parse(String id) {
        return id.indexOf(':') < 0 ? ResourceLocation.tryBuild(CreateDecals.MOD_ID, id) : ResourceLocation.tryParse(id);
    }

    /**
     * The decals this mod ships, sorted, read from our own jar's {@code assets/createdecals/decals/}. Works on a
     * dedicated server too (the assets are in the jar), so commands can suggest them.
     */
    public static synchronized List<ResourceLocation> builtIn() {
        if (builtIn == null) {
            IModFileInfo file = ModList.get().getModFileById(CreateDecals.MOD_ID);
            Path folder = file.getFile().findResource("assets", CreateDecals.MOD_ID, "decals");
            try (Stream<Path> files = Files.list(folder)) {
                builtIn = files.map(path -> path.getFileName().toString())
                        .filter(name -> name.endsWith(".json"))
                        .map(name -> id(name.substring(0, name.length() - ".json".length())))
                        .sorted()
                        .toList();
            } catch (IOException e) {
                CreateDecals.LOGGER.warn("Could not list the built-in decals in {}", folder, e);
                builtIn = List.of();
            }
        }
        return builtIn;
    }
}
