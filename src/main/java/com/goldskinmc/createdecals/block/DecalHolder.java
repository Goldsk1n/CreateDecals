package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Something that keeps decals, two layers per face: our Decal Block ({@link DecalCopycatBlockEntity}) and Create's own
 * Copycat Panel and Step and Copycats+'s copycats ({@link AttachedDecalHolder}, the decals kept beside their block
 * entity, {@link AttachedCopycats}). The stencil,
 * dye, glow and undo work through this, so they treat both alike.
 */
public interface DecalHolder {
    @Nullable
    DecalState getDecal(Direction face, boolean front);

    /** Sets or (with null) removes the decal in one layer of {@code face}; on the server it reaches every client. */
    void setDecal(Direction face, boolean front, @Nullable DecalState decal);

    /** Which faces can hold a decal. */
    boolean acceptsDecalOn(Direction face);

    /** Which layer of {@code face} is seen on top: true for the front one, false for the back one, null for a bare face. */
    @Nullable
    default Boolean topLayer(Direction face) {
        return getDecal(face, true) != null ? Boolean.TRUE : getDecal(face, false) != null ? Boolean.FALSE : null;
    }

    /**
     * The layer a stencil set to the {@code front} or back layer works on at {@code face} (picking up, erasing): that layer
     * if it has a decal, else the other one; null for a bare face.
     */
    @Nullable
    default Boolean layerFor(Direction face, boolean front) {
        if (getDecal(face, front) != null) {
            return front;
        }
        return getDecal(face, !front) != null ? !front : null;
    }

    /** The decals at {@code pos}: our Decal Block's, or an attached copycat's; null for anything else. */
    @Nullable
    static DecalHolder at(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DecalHolder holder) {
            return holder;
        }
        return be != null && AttachedCopycats.takesDecals(be.getBlockState()) ? new AttachedDecalHolder(be) : null;
    }
}
