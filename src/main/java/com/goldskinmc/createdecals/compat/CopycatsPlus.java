package com.goldskinmc.createdecals.compat;

import com.goldskinmc.createdecals.compat.copycats.CopycatsPlusBlocks;
import net.neoforged.fml.ModList;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create: Copycats+, optional. Its copycats take decals too, kept beside their block entities
 * ({@code block/AttachedDecals}) and drawn by a model of ours round theirs. Everything that touches its classes is in
 * {@code compat/copycats}, which only loads when it is installed, and uses only its {@code foundation} package, the
 * part its wiki calls public and stable (ArchitectureTest).
 */
public final class CopycatsPlus {
    public static final String MOD_ID = "copycats";
    private static final boolean LOADED = ModList.get() != null && ModList.get().isLoaded(MOD_ID);

    private CopycatsPlus() {
    }

    public static boolean loaded() {
        return LOADED;
    }

    /** Whether a block is one of Copycats+'s copycats that can take decals (not ours or Create's, not a turning one). */
    public static boolean takesDecals(BlockState state) {
        return LOADED && CopycatsPlusBlocks.takesDecals(state);
    }
}
