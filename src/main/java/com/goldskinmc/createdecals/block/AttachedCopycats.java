package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.compat.CopycatsPlus;
import com.simibubi.create.AllBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The copycats that take decals beside their own block entity ({@link AttachedDecals}, {@link AttachedDecalHolder}):
 * Create's own Copycat Panel and Copycat Step, and with Copycats+ installed its copycats. Their blocks stay theirs, so a
 * build made of them still loads without this mod, only without its decals. Our own Decal Block keeps its decals in its
 * own block entity instead ({@link DecalCopycatBlockEntity}), since Create has no full copycat block.
 */
public final class AttachedCopycats {
    private AttachedCopycats() {
    }

    public static boolean takesDecals(BlockState state) {
        return AllBlocks.COPYCAT_PANEL.has(state) || AllBlocks.COPYCAT_STEP.has(state) || CopycatsPlus.takesDecals(state);
    }

    public static boolean isCreates(Block block) {
        return block == AllBlocks.COPYCAT_PANEL.get() || block == AllBlocks.COPYCAT_STEP.get();
    }

    /** Every such block, Create's and Copycats+'s (after registration). */
    public static List<Block> blocks() {
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (takesDecals(block.defaultBlockState())) {
                blocks.add(block);
            }
        }
        return blocks;
    }
}
