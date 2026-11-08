package com.goldskinmc.createdecals.compat.copycats;

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which blocks are Copycats+'s copycats, by its public {@code ICopycatBlock} ({@code foundation} package; its wiki: "the
 * recommended way to check for a copycat block"). Create's own copycats and ours are left out (Copycats+ may give
 * Create's the same interface), and so are its turning ones (shafts, cogwheels), which a renderer draws, not the model
 * that carries decals. Loaded only when Copycats+ is installed ({@code compat/CopycatsPlus}).
 */
public final class CopycatsPlusBlocks {
    private CopycatsPlusBlocks() {
    }

    public static boolean takesDecals(BlockState state) {
        return state.getBlock() instanceof ICopycatBlock && !(state.getBlock() instanceof CopycatBlock)
                && !(state.getBlock() instanceof KineticBlock);
    }
}
