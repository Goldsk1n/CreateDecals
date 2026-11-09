package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalState;
import com.simibubi.create.AllSoundEvents;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * What a stencil click sounds and looks like, on the server so everyone near hears and sees it (a Deployer's too). A
 * decal put on sounds like dye going onto a sign and puffs a little dust of its colour off each tile it landed on; a
 * decal taken off sounds like Create's Clipboard eraser and puffs the colour it had. Sound once per click, dust on at
 * most {@link #MAX_TILES} tiles (a long word).
 */
final class DecalEffects {
    private static final int MAX_TILES = 32;
    private static final int DUST_PER_TILE = 3;

    private DecalEffects() {
    }

    static void clicked(Level level, List<DecalUndo.Change> changes) {
        if (!(level instanceof ServerLevel server) || changes.isEmpty()) {
            return;
        }
        boolean put = changes.stream().anyMatch(change -> change.after() != null);
        BlockPos at = changes.get(0).pos();
        float pitch = 0.9f + level.random.nextFloat() * 0.2f;
        if (put) {
            level.playSound(null, at, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1, pitch);
        } else {
            AllSoundEvents.CLIPBOARD_ERASE.playOnServer(level, at, 1, pitch);
        }
        int tiles = 0;
        for (DecalUndo.Change change : changes) {
            DecalState colour = put ? change.after() : change.before();
            if (colour == null) {
                continue;
            }
            dust(server, change.pos(), change.face(), colour.rgb());
            if (++tiles >= MAX_TILES) {
                break;
            }
        }
    }

    /** A few grains of {@code rgb} just off the {@code face} of the block's shape at {@code pos}. */
    private static void dust(ServerLevel level, BlockPos pos, Direction face, int rgb) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        AABB box = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
        Direction.Axis axis = face.getAxis();
        double plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.max(axis) + 0.05 : box.min(axis) - 0.05;
        double x = pos.getX() + (axis == Direction.Axis.X ? plane : (box.minX + box.maxX) / 2);
        double y = pos.getY() + (axis == Direction.Axis.Y ? plane : (box.minY + box.maxY) / 2);
        double z = pos.getZ() + (axis == Direction.Axis.Z ? plane : (box.minZ + box.maxZ) / 2);
        double spreadX = axis == Direction.Axis.X ? 0.01 : box.getXsize() * 0.3;
        double spreadY = axis == Direction.Axis.Y ? 0.01 : box.getYsize() * 0.3;
        double spreadZ = axis == Direction.Axis.Z ? 0.01 : box.getZsize() * 0.3;
        Vector3f colour = new Vector3f((rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        level.sendParticles(new DustParticleOptions(colour, 0.8f), x, y, z, DUST_PER_TILE, spreadX, spreadY, spreadZ, 0);
    }
}
