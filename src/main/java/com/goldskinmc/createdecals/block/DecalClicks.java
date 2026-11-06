package com.goldskinmc.createdecals.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Clicks both decal blocks share. */
final class DecalClicks {
    private DecalClicks() {
    }

    /**
     * A dye on a face with a decal recolours that decal, every tile of a large one (the dye is not used up, as Decision 8
     * of the plan has it).
     * Null when the click is not for us, so the copycat handles it.
     */
    @Nullable
    static ItemInteractionResult dye(ItemStack stack, Level level, BlockPos pos, Direction face) {
        if (!(stack.getItem() instanceof DyeItem dye) || !(level.getBlockEntity(pos) instanceof DecalCopycatBlockEntity be)) {
            return null;
        }
        if (be.getDecal(face) == null) {
            return null;
        }
        if (!level.isClientSide()) {
            int rgb = dye.getDyeColor().getTextureDiffuseColor();
            DecalTiles.changeWhole(level, pos, face, tile -> tile.withRgb(rgb));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
