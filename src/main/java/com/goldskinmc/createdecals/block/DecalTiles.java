package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalPlacement;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.DecalText;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Whole decals in the world: placing all their tiles at once, laying a typed word, and changing every tile of one already
 * placed. Each face has a back and a front layer ({@link DecalHolder}: our decal blocks, or Copycats+'s copycats).
 */
public final class DecalTiles {
    private DecalTiles() {
    }

    /** {@link #place(Level, BlockPos, Direction, DecalState, boolean)} on the back layer. */
    public static int place(Level level, BlockPos anchor, Direction face, DecalState decal) {
        return place(level, anchor, face, decal, false, pos -> true);
    }

    /**
     * Lays {@code decal} out from {@code anchor} (the top-left face it covers) in the {@code front} or back layer and
     * returns how many of its tiles found a face to land on; tiles over anything that takes no decals are left out.
     */
    public static int place(Level level, BlockPos anchor, Direction face, DecalState decal, boolean front, Predicate<BlockPos> allowed) {
        List<DecalPlacement.Tile> tiles = DecalPlacement.tiles(anchor, face, decal);
        return placeTiles(level, face, tiles, front, allowed);
    }

    /**
     * Lays a typed word from {@code anchor}, one letter, digit or sign per face, turned and flipped as a whole like a
     * decal one block high; spaces leave the face as it is. Returns how many letters landed, and counts in
     * {@link #letters(String)} how many there were.
     */
    public static int placeText(Level level, BlockPos anchor, Direction face, String text, int rgb, int rotation, boolean flip,
            boolean glow, boolean front, Predicate<BlockPos> allowed) {
        return placeTiles(level, face, textTiles(anchor, face, text, rgb, rotation, flip, glow), front, allowed);
    }

    /** Where each letter of a typed word goes (spaces left out). */
    public static List<DecalPlacement.Tile> textTiles(BlockPos anchor, Direction face, String text, int rgb, int rotation, boolean flip,
            boolean glow) {
        List<DecalPlacement.Tile> tiles = new ArrayList<>();
        String word = DecalText.normalise(text);
        for (int i = 0; i < word.length(); i++) {
            ResourceLocation id = DecalText.decalFor(word.charAt(i));
            if (id == null) {
                continue;
            }
            int[] cell = DecalPlacement.cellOf(word.length(), 1, rotation, flip, i, 0);
            tiles.add(new DecalPlacement.Tile(DecalPlacement.cell(anchor, face, cell[0], cell[1]),
                    new DecalState(id, rgb, rotation, flip, 0, 0, 1, 1, glow)));
        }
        return tiles;
    }

    /** How many faces a typed word covers (its letters, digits and signs; not its spaces). */
    public static int letters(String text) {
        String word = DecalText.normalise(text);
        int count = 0;
        for (int i = 0; i < word.length(); i++) {
            if (DecalText.decalFor(word.charAt(i)) != null) {
                count++;
            }
        }
        return count;
    }

    /** Tiles on blocks {@code allowed} rejects (protected, claimed: {@link DecalEdits}) are left out, as missing ones are. */
    private static int placeTiles(Level level, Direction face, List<DecalPlacement.Tile> tiles, boolean front, Predicate<BlockPos> allowed) {
        int placed = 0;
        for (DecalPlacement.Tile tile : tiles) {
            if (!allowed.test(tile.pos())) {
                continue;
            }
            DecalHolder be = DecalHolder.at(level, tile.pos());
            if (be != null && be.acceptsDecalOn(face)) {
                DecalState before = be.getDecal(face, front);
                be.setDecal(face, front, tile.decal());
                DecalUndo.changed(tile.pos(), face, front, before, tile.decal());
                placed++;
            }
        }
        return placed;
    }

    /**
     * Applies {@code change} to every tile of the decal seen on top of the face at {@code pos} (its front decal if it has
     * one, else its back one; a one-block decal is one tile); {@code change} returning null removes the tile. Returns how
     * many tiles changed.
     */
    public static int changeWhole(Level level, BlockPos pos, Direction face, UnaryOperator<@Nullable DecalState> change) {
        return changeWhole(level, pos, face, change, p -> true);
    }

    /** {@link #changeWhole(Level, BlockPos, Direction, UnaryOperator)}, leaving out tiles {@code allowed} rejects. */
    public static int changeWhole(Level level, BlockPos pos, Direction face, UnaryOperator<@Nullable DecalState> change,
            Predicate<BlockPos> allowed) {
        DecalHolder clicked = DecalHolder.at(level, pos);
        if (clicked == null) {
            return 0;
        }
        Boolean front = clicked.topLayer(face);
        return front == null ? 0 : changeWhole(level, pos, face, front, change, allowed);
    }

    /** {@link #changeWhole(Level, BlockPos, Direction, UnaryOperator)} on one layer. */
    public static int changeWhole(Level level, BlockPos pos, Direction face, boolean front, UnaryOperator<@Nullable DecalState> change) {
        return changeWhole(level, pos, face, front, change, p -> true);
    }

    /** {@link #changeWhole(Level, BlockPos, Direction, boolean, UnaryOperator)}, leaving out tiles {@code allowed} rejects. */
    public static int changeWhole(Level level, BlockPos pos, Direction face, boolean front, UnaryOperator<@Nullable DecalState> change,
            Predicate<BlockPos> allowed) {
        DecalHolder clicked = DecalHolder.at(level, pos);
        if (clicked == null || clicked.getDecal(face, front) == null) {
            return 0;
        }
        DecalState shown = clicked.getDecal(face, front);
        BlockPos anchor = DecalPlacement.anchorOf(pos, face, shown);
        int changed = 0;
        for (DecalPlacement.Tile tile : DecalPlacement.tiles(anchor, face, shown)) {
            DecalHolder be = allowed.test(tile.pos()) ? DecalHolder.at(level, tile.pos()) : null;
            if (be != null) {
                DecalState there = be.getDecal(face, front);
                if (there != null && there.sameDecalAs(shown) && there.tileX() == tile.decal().tileX()
                        && there.tileY() == tile.decal().tileY()) {
                    DecalState after = change.apply(there);
                    be.setDecal(face, front, after);
                    DecalUndo.changed(tile.pos(), face, front, there, after);
                    changed++;
                }
            }
        }
        return changed;
    }
}
