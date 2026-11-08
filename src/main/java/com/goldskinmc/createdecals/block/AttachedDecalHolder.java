package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.registry.DecalsAttachments;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The decals on a Copycats+ copycat, kept in an attachment on its block entity ({@link AttachedDecals}). Any face may
 * hold one: the model projects it onto whatever part of the copycat faces that way (a slope's slanted top included), so
 * a face the shape lacks shows nothing.
 */
public record AttachedDecalHolder(BlockEntity be) implements DecalHolder {
    public AttachedDecals decals() {
        return be.getExistingData(DecalsAttachments.DECALS.get()).orElse(AttachedDecals.EMPTY);
    }

    @Override
    @Nullable
    public DecalState getDecal(Direction face, boolean front) {
        return decals().get(face, front);
    }

    @Override
    public void setDecal(Direction face, boolean front, @Nullable DecalState decal) {
        AttachedDecals before = decals();
        if (Objects.equals(before.get(face, front), decal)) {
            return;
        }
        set(be, before.with(face, front, decal));
    }

    @Override
    public boolean acceptsDecalOn(Direction face) {
        return true;
    }

    /** Replaces all of a copycat's decals: on the server it is saved and sent to the players who see it; on a client it is redrawn. */
    public static void set(BlockEntity be, AttachedDecals decals) {
        if (decals.isEmpty()) {
            be.removeData(DecalsAttachments.DECALS.get());
        } else {
            be.setData(DecalsAttachments.DECALS.get(), decals);
        }
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        if (!level.isClientSide()) {
            be.setChanged();
            DecalSync.send(be);
        } else {
            be.requestModelDataUpdate();
            level.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 8);
        }
    }
}
