package com.goldskinmc.createdecals;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The v1.0 acceptance criterion "every block has a recipe and en_us text" (Ponder scenes join at M6), checked on the
 * datagen output in {@code src/generated/resources}: run {@code runData} after adding a block. Also: every built-in
 * decal has both its sprite and its definition.
 */
class ContentCompletenessTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path ASSETS = GENERATED.resolve("assets/createdecals");
    private static final Path DATA = GENERATED.resolve("data/createdecals");
    private static final Path OWN_ASSETS = Path.of("src/main/resources/assets/createdecals");

    @Test
    void everyBlockHasNameTooltipRecipeAndLoot() throws IOException {
        String lang = Files.readString(ASSETS.resolve("lang/en_us.json"));
        List<String> missing = new ArrayList<>();
        List<String> blocks = names(ASSETS.resolve("blockstates"), ".json");
        assertTrue(blocks.size() >= 2, "expected at least 2 blocks, found " + blocks);
        for (String block : blocks) {
            requireKey(lang, "block.createdecals." + block, missing);
            requireKey(lang, "block.createdecals." + block + ".tooltip.summary", missing);
            requireFile(DATA.resolve("recipe/" + block + ".json"), missing);
            requireFile(DATA.resolve("loot_table/blocks/" + block + ".json"), missing);
            requireFile(ASSETS.resolve("models/item/" + block + ".json"), missing);
        }
        assertTrue(missing.isEmpty(), "missing: " + missing);
    }

    @Test
    void everyItemHasNameTooltipRecipeAndModel() throws IOException {
        String lang = Files.readString(ASSETS.resolve("lang/en_us.json"));
        List<String> missing = new ArrayList<>();
        for (String item : List.of("decal_stencil")) {
            requireKey(lang, "item.createdecals." + item, missing);
            requireKey(lang, "item.createdecals." + item + ".tooltip.summary", missing);
            requireFile(DATA.resolve("recipe/" + item + ".json"), missing);
            requireFile(ASSETS.resolve("models/item/" + item + ".json"), missing);
        }
        assertTrue(missing.isEmpty(), "missing: " + missing);
    }

    @Test
    void everyDecalHasSpriteAndDefinition() throws IOException {
        List<String> definitions = names(OWN_ASSETS.resolve("decals"), ".json");
        List<String> sprites = names(OWN_ASSETS.resolve("textures/decal"), ".png");
        assertTrue(definitions.size() >= 27, "expected at least 27 decals, found " + definitions);
        assertTrue(definitions.equals(sprites), "definitions " + definitions + " and sprites " + sprites + " differ");
    }

    private static List<String> names(Path folder, String extension) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(extension))
                    .map(name -> name.substring(0, name.length() - extension.length()))
                    .sorted()
                    .toList();
        }
    }

    private static void requireKey(String lang, String key, List<String> missing) {
        if (!lang.contains("\"" + key + "\":")) {
            missing.add(key);
        }
    }

    private static void requireFile(Path file, List<String> missing) {
        if (!Files.isRegularFile(file)) {
            missing.add(file.toString());
        }
    }
}
