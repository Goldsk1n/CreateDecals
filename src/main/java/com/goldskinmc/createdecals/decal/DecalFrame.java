package com.goldskinmc.createdecals.decal;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * How a decal lies on a face, in world directions. Side faces stand upright; the top and bottom read with north
 * (top face) or south (bottom face) away from the viewer, as vanilla's textures do. A decal turned {@code r}
 * quarter turns clockwise has its top pointing {@link #top}.
 */
public final class DecalFrame {
    private DecalFrame() {
    }

    /** The direction that is up on an unturned decal on the face pointing {@code facing}. */
    public static Direction up(Direction facing) {
        return switch (facing) {
            case UP -> Direction.NORTH;
            case DOWN -> Direction.SOUTH;
            default -> Direction.UP;
        };
    }

    /** The direction that is right to someone looking at the face pointing {@code facing}. */
    public static Direction right(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case SOUTH, UP, DOWN -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.SOUTH;
        };
    }

    /** Where the top of a decal turned {@code rotation} quarter turns clockwise points. */
    public static Direction top(Direction facing, int rotation) {
        return switch (Math.floorMod(rotation, 4)) {
            case 0 -> up(facing);
            case 1 -> right(facing);
            case 2 -> up(facing).getOpposite();
            default -> right(facing).getOpposite();
        };
    }

    /** The rotation that points a decal's top {@code top} on the face pointing {@code facing}, or null if it cannot. */
    @Nullable
    public static Integer rotationFor(Direction facing, Direction top) {
        for (int r = 0; r < 4; r++) {
            if (top(facing, r) == top) {
                return r;
            }
        }
        return null;
    }
}
