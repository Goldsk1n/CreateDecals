package com.goldskinmc.createdecals.registry;

import static com.goldskinmc.createdecals.CreateDecals.REGISTRATE;

import com.goldskinmc.createdecals.block.DecalBlock;
import com.goldskinmc.createdecals.block.DecalPanelBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * The two decal blocks. Both are Create copycats: their block model is empty air and {@code client/DecalsClient} puts
 * the decal models round it; their item models are Create's copycat item models, by resource location.
 */
public final class DecalsBlocks {
    public static final BlockEntry<DecalPanelBlock> DECAL_PANEL = REGISTRATE
            .block("decal_panel", DecalPanelBlock::new)
            .transform(copycat("panel"))
            .lang("Decal Panel")
            .register();

    public static final BlockEntry<DecalBlock> DECAL_BLOCK = REGISTRATE
            .block("decal_block", DecalBlock::new)
            .transform(copycat("block"))
            .lang("Decal Block")
            .register();

    private DecalsBlocks() {
    }

    /** What Create's own copycats get (BuilderTransformers.copycat), minus its colour handler: ours tints decals too. */
    private static <B extends CopycatBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> copycat(String createItemModel) {
        return b -> b.initialProperties(SharedProperties::softMetal)
                .properties(p -> p.noOcclusion()
                        .mapColor(MapColor.NONE)
                        .isValidSpawn((state, level, pos, type) -> false))
                .blockstate((c, p) -> p.simpleBlock(c.get(), p.models().getExistingFile(p.mcLoc("block/air"))))
                .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE)
                .item()
                // Create's copycat item models; datagen cannot see Create's assets to check them, Create always ships them
                .model((c, p) -> p.getBuilder(c.getName()).parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("create", "block/copycat_base/" + createItemModel))))
                .build();
    }

    public static void register() {
        // Loading this class registers the entries above
    }
}
