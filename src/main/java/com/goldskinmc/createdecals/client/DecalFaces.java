package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.decal.DecalFrame;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import com.simibubi.create.content.decoration.copycat.CopycatStepBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.jetbrains.annotations.Nullable;

/**
 * Where a decal lies on each face of the decal blocks. A full block's face is the whole face of the block; a panel's
 * front is the whole face, 3/16 out from its back; a step's faces are windows onto the decal as it would lie on the
 * block's face in that plane, so a step shows the part of the decal it covers (a bottom step's front the decal's lower
 * half) and lines up with Decal Blocks and other steps beside it.
 */
public final class DecalFaces {
    private static final float PANEL_FRONT = 3 / 16f;

    /**
     * Where on a face the decal shows: {@code depth} blocks from the block's far side along the face's normal (as
     * {@link DecalQuads#quad} takes it), and the part of the face, as fractions of the decal from its left
     * ({@code s0}-{@code s1}) and top ({@code t0}-{@code t1}).
     */
    public record Window(float depth, float s0, float s1, float t0, float t1) {
        public static final Window FULL = new Window(1, 0, 1, 0, 1);

        public boolean whole() {
            return s0 == 0 && s1 == 1 && t0 == 0 && t1 == 1;
        }

        /** On the block's boundary, so a solid neighbour hides it. */
        public boolean onBoundary() {
            return depth == 1;
        }
    }

    private DecalFaces() {
    }

    /** The window for a decal on {@code face}, or null where this block takes none. */
    @Nullable
    public static Window window(BlockState state, Direction face) {
        if (state.hasProperty(CopycatPanelBlock.FACING)) {
            return state.getValue(CopycatPanelBlock.FACING) == face ? new Window(PANEL_FRONT, 0, 1, 0, 1) : null;
        }
        if (state.hasProperty(CopycatStepBlock.FACING) && state.hasProperty(CopycatStepBlock.HALF)) {
            return stepWindow(state.getValue(CopycatStepBlock.FACING), state.getValue(CopycatStepBlock.HALF) == Half.TOP, face);
        }
        return Window.FULL;
    }

    /**
     * A step is a quarter of a block: full width, the lower or upper half, and the half towards {@code facing} (Create's
     * {@code AllShapes.STEP_BOTTOM}/{@code STEP_TOP}). Each of its faces is the matching part of the block's face plane.
     */
    static Window stepWindow(Direction facing, boolean top, Direction face) {
        float[] min = {0, top ? 0.5f : 0, 0};
        float[] max = {1, top ? 1 : 0.5f, 1};
        int along = facing.getAxis().ordinal();
        if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            min[along] = 0.5f;
        } else {
            max[along] = 0.5f;
        }
        int axis = face.getAxis().ordinal();
        float depth = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? max[axis] : 1 - min[axis];
        Direction right = DecalFrame.right(face);
        Direction up = DecalFrame.up(face);
        int r = right.getAxis().ordinal();
        int u = up.getAxis().ordinal();
        boolean rightPositive = right.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        boolean upPositive = up.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        float s0 = rightPositive ? min[r] : 1 - max[r];
        float s1 = rightPositive ? max[r] : 1 - min[r];
        float t0 = upPositive ? 1 - max[u] : min[u];
        float t1 = upPositive ? 1 - min[u] : max[u];
        return new Window(depth, s0, s1, t0, t1);
    }
}
