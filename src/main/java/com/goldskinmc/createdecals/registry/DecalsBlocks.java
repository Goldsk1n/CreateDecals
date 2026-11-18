package com.goldskinmc.createdecals.registry;

import com.goldskinmc.createdecals.block.DecalBlock;
import com.goldskinmc.createdecals.contraption.DecalMovement;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import static com.goldskinmc.createdecals.CreateDecals.REGISTRATE;

/**
 * The Decal Block, a full Create copycat (Create has none): its block model is empty air and {@code client/DecalsClient}
 * puts the decal model round it; its item model is Create's copycat item model, by resource location. Create's own
 * Copycat Panel and Step take decals as they are ({@code block/AttachedCopycats}).
 */
public final class DecalsBlocks {
    public static final BlockEntry<DecalBlock> DECAL_BLOCK = REGISTRATE
            .block("decal_block", DecalBlock::new)
            .transform(copycat("block"))
            .lang("Decal Block")
            .onRegister(MovementBehaviour.movementBehaviour(new DecalMovement()))
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
