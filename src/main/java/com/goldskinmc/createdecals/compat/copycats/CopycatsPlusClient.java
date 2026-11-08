package com.goldskinmc.createdecals.compat.copycats;

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import net.minecraft.client.color.block.BlockColor;

/**
 * Client side of Copycats+ (loaded only when it is installed, {@code compat/CopycatsPlus}): the colour its copycats give
 * their material (grass, leaves), from its public {@code ICopycatBlock.wrappedColor()}, which our colour handler hands
 * every tint that is not a decal's.
 */
public final class CopycatsPlusClient {
    private CopycatsPlusClient() {
    }

    public static BlockColor materialColour() {
        return ICopycatBlock.wrappedColor();
    }
}
