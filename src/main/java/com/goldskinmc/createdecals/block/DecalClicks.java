package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalState;
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
     * A dye on a face with a decal recolours that decal (the dye is not used up, as Decision 8 of the plan has it).
     * Null when the click is not for us, so the copycat handles it.
     */
    @Nullable
    static ItemInteractionResult dye(ItemStack stack, Level level, BlockPos pos, Direction face) {
        if (!(stack.getItem() instanceof DyeItem dye) || !(level.getBlockEntity(pos) instanceof DecalCopycatBlockEntity be)) {
            return null;
        }
        DecalState decal = be.getDecal(face);
        if (decal == null) {
            return null;
        }
        if (!level.isClientSide()) {
            be.setDecal(face, decal.withRgb(dye.getDyeColor().getTextureDiffuseColor()));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
