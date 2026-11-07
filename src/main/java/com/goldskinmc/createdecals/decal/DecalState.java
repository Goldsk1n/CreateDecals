package com.goldskinmc.createdecals.decal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * One decal on one face: which decal ({@link Decals}), its colour (0xRRGGBB), how it lies on the face
 * ({@code flip} mirrors it left to right, then it is turned {@code rotation} quarter turns clockwise), and for decals larger
 * than one block, its size in tiles ({@code width} x {@code height}, as drawn) and which tile this face shows
 * ({@code tileX} from the left, {@code tileY} from the top, as drawn). The size is kept here because only clients know
 * the decal library: the server needs it to find the other tiles (erasing, recolouring). A {@code glow}ing decal is drawn
 * full-bright (a Glow Ink Sac), readable at night.
 */
public record DecalState(ResourceLocation id, int rgb, int rotation, boolean flip, int tileX, int tileY, int width, int height,
        boolean glow) {
    public DecalState {
        rgb &= 0xFFFFFF;
        rotation = Math.floorMod(rotation, 4);
        width = Math.max(1, Math.min(DecalPlacement.MAX_SIZE, width));
        height = Math.max(1, Math.min(DecalPlacement.MAX_SIZE, height));
        tileX = Math.max(0, Math.min(width - 1, tileX));
        tileY = Math.max(0, Math.min(height - 1, tileY));
    }

    /** A decal that does not glow. */
    public DecalState(ResourceLocation id, int rgb, int rotation, boolean flip, int tileX, int tileY, int width, int height) {
        this(id, rgb, rotation, flip, tileX, tileY, width, height, false);
    }

    /** A one-block decal. */
    public DecalState(ResourceLocation id, int rgb, int rotation, boolean flip) {
        this(id, rgb, rotation, flip, 0, 0, 1, 1);
    }

    public static DecalState of(ResourceLocation id, DyeColor dye) {
        return new DecalState(id, dye.getTextureDiffuseColor(), 0, false);
    }

    public DecalState withRgb(int newRgb) {
        return new DecalState(id, newRgb, rotation, flip, tileX, tileY, width, height, glow);
    }

    public DecalState withTurn(int newRotation, boolean newFlip) {
        return new DecalState(id, rgb, newRotation, newFlip, tileX, tileY, width, height, glow);
    }

    public DecalState withTile(int newTileX, int newTileY) {
        return new DecalState(id, rgb, rotation, flip, newTileX, newTileY, width, height, glow);
    }

    public DecalState withGlow(boolean newGlow) {
        return new DecalState(id, rgb, rotation, flip, tileX, tileY, width, height, newGlow);
    }

    /** Whether this face shows part of the same decal as {@code other} (same decal, size, turn and flip), any tile. */
    public boolean sameDecalAs(DecalState other) {
        return id.equals(other.id) && rotation == other.rotation && flip == other.flip && width == other.width && height == other.height;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id.toString());
        tag.putInt("Rgb", rgb);
        tag.putByte("Rot", (byte) rotation);
        tag.putBoolean("Flip", flip);
        if (glow) {
            tag.putBoolean("Glow", true);
        }
        if (width != 1 || height != 1) {
            tag.putByte("W", (byte) width);
            tag.putByte("H", (byte) height);
            tag.putByte("TileX", (byte) tileX);
            tag.putByte("TileY", (byte) tileY);
        }
        return tag;
    }

    public static DecalState load(CompoundTag tag) {
        return new DecalState(Decals.parse(tag.getString("Id")), tag.getInt("Rgb"), tag.getByte("Rot"), tag.getBoolean("Flip"),
                tag.getByte("TileX"), tag.getByte("TileY"), Math.max(1, tag.getByte("W")), Math.max(1, tag.getByte("H")),
                tag.getBoolean("Glow"));
    }
}
