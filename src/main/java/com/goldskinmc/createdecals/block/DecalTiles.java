package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalPlacement;
import com.goldskinmc.createdecals.decal.DecalState;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Whole decals in the world: placing all their tiles at once, and changing every tile of one already placed. */
public final class DecalTiles {
    private DecalTiles() {
    }

    /**
     * Lays {@code decal} out from {@code anchor} (the top-left face it covers) and returns how many of its tiles found a
     * face to land on; tiles over anything but a decal block's free face are left out. Create's own Copycat Panels under
     * a tile become Decal Panels ({@link CreatePanels}).
     */
    public static int place(Level level, BlockPos anchor, Direction face, DecalState decal) {
        int placed = 0;
        for (DecalPlacement.Tile tile : DecalPlacement.tiles(anchor, face, decal)) {
            DecalCopycatBlockEntity be = level.getBlockEntity(tile.pos()) instanceof DecalCopycatBlockEntity ours ? ours
                    : CreatePanels.convert(level, tile.pos(), face);
            if (be != null && be.acceptsDecalOn(face)) {
                be.setDecal(face, tile.decal());
                placed++;
            }
        }
        return placed;
    }

    /**
     * Applies {@code change} to every tile of the decal the face at {@code pos} shows (a one-block decal is one tile);
     * {@code change} returning null removes the tile. Returns how many tiles changed.
     */
    public static int changeWhole(Level level, BlockPos pos, Direction face, UnaryOperator<@Nullable DecalState> change) {
        if (!(level.getBlockEntity(pos) instanceof DecalCopycatBlockEntity clicked) || clicked.getDecal(face) == null) {
            return 0;
        }
        DecalState shown = clicked.getDecal(face);
        BlockPos anchor = DecalPlacement.anchorOf(pos, face, shown);
        int changed = 0;
        for (DecalPlacement.Tile tile : DecalPlacement.tiles(anchor, face, shown)) {
            if (level.getBlockEntity(tile.pos()) instanceof DecalCopycatBlockEntity be) {
                DecalState there = be.getDecal(face);
                if (there != null && there.sameDecalAs(shown) && there.tileX() == tile.decal().tileX()
                        && there.tileY() == tile.decal().tileY()) {
                    be.setDecal(face, change.apply(there));
                    changed++;
                }
            }
        }
        return changed;
    }
}
