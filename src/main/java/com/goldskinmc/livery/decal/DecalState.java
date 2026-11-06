package com.goldskinmc.livery.decal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/**
 * One decal on a panel: which sprite, its colour (0xRRGGBB, usually a dye's), and how it lies on the face:
 * {@code rotation} quarter turns clockwise, then {@code flip} mirrors it left to right.
 */
public record DecalState(Decal decal, int rgb, int rotation, boolean flip) {
    public DecalState {
        rgb &= 0xFFFFFF;
        rotation = Math.floorMod(rotation, 4);
    }

    public static DecalState of(Decal decal, DyeColor dye) {
        return new DecalState(decal, dye.getTextureDiffuseColor(), 0, false);
    }

    public DecalState withRgb(int newRgb) {
        return new DecalState(decal, newRgb, rotation, flip);
    }

    public DecalState rotated(int quarterTurns) {
        return new DecalState(decal, rgb, rotation + quarterTurns, flip);
    }

    /** Everything that changes the baked quad apart from the face, in one number (for the quad cache). */
    public long key() {
        return ((long) decal.ordinal() << 32) | ((long) rgb << 3) | ((long) rotation << 1) | (flip ? 1 : 0);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", decal.id());
        tag.putInt("Rgb", rgb);
        tag.putByte("Rot", (byte) rotation);
        tag.putBoolean("Flip", flip);
        return tag;
    }

    @Nullable
    public static DecalState load(CompoundTag tag) {
        Decal decal = Decal.byId(tag.getString("Id"));
        if (decal == null) {
            return null;
        }
        return new DecalState(decal, tag.getInt("Rgb"), tag.getByte("Rot"), tag.getBoolean("Flip"));
    }
}
