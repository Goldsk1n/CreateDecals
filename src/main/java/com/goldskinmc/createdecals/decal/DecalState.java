package com.goldskinmc.createdecals.decal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * One decal on one face: which decal ({@link Decals}), its colour (0xRRGGBB), how it lies on the face
 * ({@code rotation} quarter turns clockwise, then {@code flip} mirrors it left to right), and for decals larger than
 * one block which tile of it this face shows ({@code tileX} from the left, {@code tileY} from the top, before turning).
 */
public record DecalState(ResourceLocation id, int rgb, int rotation, boolean flip, int tileX, int tileY) {
    public DecalState {
        rgb &= 0xFFFFFF;
        rotation = Math.floorMod(rotation, 4);
        tileX = Math.max(0, tileX);
        tileY = Math.max(0, tileY);
    }

    public DecalState(ResourceLocation id, int rgb, int rotation, boolean flip) {
        this(id, rgb, rotation, flip, 0, 0);
    }

    public static DecalState of(ResourceLocation id, DyeColor dye) {
        return new DecalState(id, dye.getTextureDiffuseColor(), 0, false);
    }

    public DecalState withRgb(int newRgb) {
        return new DecalState(id, newRgb, rotation, flip, tileX, tileY);
    }

    public DecalState withTurn(int newRotation, boolean newFlip) {
        return new DecalState(id, rgb, newRotation, newFlip, tileX, tileY);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id.toString());
        tag.putInt("Rgb", rgb);
        tag.putByte("Rot", (byte) rotation);
        tag.putBoolean("Flip", flip);
        if (tileX != 0 || tileY != 0) {
            tag.putByte("TileX", (byte) tileX);
            tag.putByte("TileY", (byte) tileY);
        }
        return tag;
    }

    public static DecalState load(CompoundTag tag) {
        return new DecalState(Decals.parse(tag.getString("Id")), tag.getInt("Rgb"), tag.getByte("Rot"), tag.getBoolean("Flip"),
                tag.getByte("TileX"), tag.getByte("TileY"));
    }
}
