package com.goldskinmc.createdecals.decal;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Where the tiles of a decal larger than one block go. A decal is drawn {@code width} x {@code height} tiles; flipped
 * and turned on a face it covers a rectangle of faces on one plane, anchored at its top-left face (as seen on the
 * face, in {@link DecalFrame}'s right and up). Each face then shows one tile, itself flipped and turned the same way,
 * so the whole decal reads as one picture.
 */
public final class DecalPlacement {
    /** Largest decal side, in blocks. */
    public static final int MAX_SIZE = 4;

    /** One face of a placed decal: the block and the decal (with its tile) it shows. */
    public record Tile(BlockPos pos, DecalState decal) {
    }

    private DecalPlacement() {
    }

    /**
     * Where tile ({@code tileX}, {@code tileY}) of a decal shows once flipped and turned: its cell (right, down) in the
     * rectangle the decal covers, counted from the rectangle's top-left.
     */
    public static int[] cellOf(DecalState decal, int tileX, int tileY) {
        int w = decal.width();
        int h = decal.height();
        int x = decal.flip() ? w - 1 - tileX : tileX;
        int y = tileY;
        for (int i = 0; i < decal.rotation(); i++) {
            // A quarter turn clockwise: a W x H grid becomes H x W, its top-left cell going to the top right
            int turnedX = h - 1 - y;
            y = x;
            x = turnedX;
            int swap = w;
            w = h;
            h = swap;
        }
        return new int[] {x, y};
    }

    /** The face {@code right} cells right and {@code down} cells down of {@code anchor}, on the face pointing {@code face}. */
    public static BlockPos cell(BlockPos anchor, Direction face, int right, int down) {
        return anchor.relative(DecalFrame.right(face), right).relative(DecalFrame.up(face), -down);
    }

    /** Every tile of {@code decal} (whatever its tile) laid out from {@code anchor}, the top-left face it covers. */
    public static List<Tile> tiles(BlockPos anchor, Direction face, DecalState decal) {
        List<Tile> tiles = new ArrayList<>();
        for (int ty = 0; ty < decal.height(); ty++) {
            for (int tx = 0; tx < decal.width(); tx++) {
                int[] cell = cellOf(decal, tx, ty);
                tiles.add(new Tile(cell(anchor, face, cell[0], cell[1]), decal.withTile(tx, ty)));
            }
        }
        return tiles;
    }

    /** The top-left face of the decal a face at {@code pos} shows one tile of. */
    public static BlockPos anchorOf(BlockPos pos, Direction face, DecalState tile) {
        int[] cell = cellOf(tile, tile.tileX(), tile.tileY());
        return cell(pos, face, -cell[0], -cell[1]);
    }
}
