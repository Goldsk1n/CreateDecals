package com.goldskinmc.createdecals.block;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Whether a player may change the decals on a block. The game checks only the block a player clicks; a large decal, a
 * typed word, or the eraser and dye on one reach the blocks beside it too, so each of those is asked here: spawn
 * protection and the world border ({@code mayInteract}), adventure mode ({@code mayUseItemAt}), and protection and claim
 * mods through the block-placing event they already cancel for blocks a player may not build on (a decal changes the
 * block as a placement would). Without a player (commands, tests) everything may change.
 */
public final class DecalEdits {
    private DecalEdits() {
    }

    public static boolean mayEdit(@Nullable Player player, Level level, BlockPos pos, Direction face, ItemStack held) {
        if (player == null) {
            return true;
        }
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, held)) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }
        BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos),
                level.getBlockState(pos), player);
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    /**
     * The face a click is for. A Deployer pointing down (or up) on a moving contraption clicks with a level ray, so Create
     * hands us the side of the block it reaches, though it means the top (or bottom); its fake player looks the way the
     * Deployer points, as a standing Deployer's does (whose ray already finds that face).
     */
    public static Direction clickedFace(@Nullable Player player, Direction clicked) {
        if (player instanceof FakePlayer && Math.abs(player.getXRot()) > 89) {
            return player.getXRot() > 0 ? Direction.UP : Direction.DOWN;
        }
        return clicked;
    }

    /** {@link #mayEdit} for every block a click reaches. */
    public static Predicate<BlockPos> by(@Nullable Player player, Level level, Direction face, ItemStack held) {
        return pos -> mayEdit(player, level, pos, face, held);
    }
}
