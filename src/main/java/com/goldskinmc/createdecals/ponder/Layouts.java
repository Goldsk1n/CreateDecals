package com.goldskinmc.createdecals.ponder;

import net.minecraft.core.BlockPos;

/**
 * Where things stand in each Ponder scene, shared by the schematic generator ({@link PonderSchematics}) and the scenes
 * ({@link DecalsScenes}). Layer 0 is the base plate. Ponder's camera looks from the north-west, so decals go on north
 * faces; seen from the north, a decal's right is west (smaller x).
 */
final class Layouts {
    /** 5x5 plate: a short stone-brick wall with one Decal Panel on its north side. */
    static final class Panel {
        static final int SIZE = 5;
        static final BlockPos WALL_FROM = new BlockPos(1, 1, 3);
        static final BlockPos WALL_TO = new BlockPos(3, 2, 3);
        static final BlockPos PANEL = new BlockPos(2, 1, 2);
    }

    /** 5x5 plate: one Decal Block in the middle. */
    static final class Block {
        static final int SIZE = 5;
        static final BlockPos BLOCK = new BlockPos(2, 1, 2);
    }

    /** 5x5 plate: a Decal Block, and west of it a bottom Decal Step facing north, its front in the block's north face's plane. */
    static final class Step {
        static final int SIZE = 5;
        static final BlockPos BLOCK = new BlockPos(3, 1, 2);
        static final BlockPos STEP = new BlockPos(2, 1, 2);
    }

    /**
     * 7x7 plate: one Decal Block on the left as Ponder shows it, where the scene starts, and a 3x3 wall of Decal Blocks
     * on the right, both facing north and one block in from the plate's edges.
     */
    static final class Stencil {
        static final int SIZE = 7;
        static final BlockPos WALL_FROM = new BlockPos(1, 1, 3);
        static final BlockPos WALL_TO = new BlockPos(3, 3, 3);
        /** Seen from the north, the wall's top-left face: where a 3x3 decal is clicked on. */
        static final BlockPos TOP_LEFT = new BlockPos(3, 3, 3);
        static final BlockPos MIDDLE = new BlockPos(2, 2, 3);
        static final BlockPos SIDE = new BlockPos(5, 1, 3);
    }

    /**
     * 5x5 plate: a row of three Decal Blocks running east, a Deployer two blocks over the middle one pointing down, its
     * shaft running south to the plate's edge.
     */
    static final class Deployer {
        static final int SIZE = 5;
        static final BlockPos ROW_FROM = new BlockPos(1, 1, 2);
        static final BlockPos ROW_TO = new BlockPos(3, 1, 2);
        static final BlockPos MIDDLE = new BlockPos(2, 1, 2);
        static final BlockPos DEPLOYER = new BlockPos(2, 3, 2);
        static final BlockPos SHAFT_FROM = new BlockPos(2, 3, 3);
        static final BlockPos SHAFT_TO = new BlockPos(2, 3, 4);
    }

    private Layouts() {
    }
}
