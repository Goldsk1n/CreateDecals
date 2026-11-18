package com.goldskinmc.createdecals.registry;

import static com.goldskinmc.createdecals.CreateDecals.REGISTRATE;

import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

/** One block entity type for every decal block: a panel is the same thing with only its front face in use. */
public final class DecalsBlockEntities {
    public static final BlockEntityEntry<DecalCopycatBlockEntity> DECAL_COPYCAT = REGISTRATE
            .blockEntity("decal_copycat", DecalCopycatBlockEntity::new)
            .validBlocks(DecalsBlocks.DECAL_BLOCK)
            .register();

    private DecalsBlockEntities() {
    }

    public static void register() {
        // Loading this class registers the entries above
    }
}
