package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.decal.DecalState;
import com.simibubi.create.AllSoundEvents;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Undo for stencil clicks: each click that changes decals (a decal, a large one, a word, the eraser) is one step, the
 * tiles it changed with what they held before and after. A player's last {@link #STEPS} steps are kept in memory on the
 * server and forgotten when they leave or the server stops. Undoing puts a tile back only if it still holds what the
 * click left there (nobody has painted over it, it was not mined or carried off on a contraption) and the player may
 * still edit it ({@link DecalEdits}); the rest are skipped and counted.
 */
@EventBusSubscriber(modid = CreateDecals.MOD_ID)
public final class DecalUndo {
    /** How many clicks back a player can undo. */
    public static final int STEPS = 10;

    /** One tile a click changed: which layer of which face, and what it held before and after (null: no decal). */
    record Change(BlockPos pos, Direction face, boolean front, @Nullable DecalState before, @Nullable DecalState after) {
    }

    record Step(ResourceKey<Level> dimension, List<Change> changes) {
    }

    /** What undoing did: tiles put back, and how many the step had. */
    public record Result(int restored, int total) {
    }

    private static final Map<UUID, Deque<Step>> HISTORY = new HashMap<>();
    /** The changes of the click being made on this thread, or null when no click is being recorded. */
    private static final ThreadLocal<List<Change>> RECORDING = new ThreadLocal<>();

    private DecalUndo() {
    }

    /**
     * Runs a click's changes, keeping them as one step the {@code player} can undo (if any tile changed), and plays the
     * click's sound and dust ({@link DecalEffects}). A Deployer's clicks are not kept: its fake player answers to its
     * owner's UUID, so they would land in the owner's own undo.
     */
    public static <T> T record(@Nullable Player player, Level level, Supplier<T> click) {
        if (level.isClientSide()) {
            return click.get();
        }
        List<Change> changes = new ArrayList<>();
        RECORDING.set(changes);
        try {
            return click.get();
        } finally {
            RECORDING.remove();
            DecalEffects.clicked(level, changes);
            if (!changes.isEmpty() && player != null && !(player instanceof FakePlayer)) {
                Deque<Step> steps = HISTORY.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
                steps.push(new Step(level.dimension(), List.copyOf(changes)));
                while (steps.size() > STEPS) {
                    steps.removeLast();
                }
            }
        }
    }

    /** Called by {@link DecalTiles} for every tile it changes; kept only while a click is being recorded. */
    static void changed(BlockPos pos, Direction face, boolean front, @Nullable DecalState before, @Nullable DecalState after) {
        List<Change> changes = RECORDING.get();
        if (changes != null && !Objects.equals(before, after)) {
            changes.add(new Change(pos.immutable(), face, front, before, after));
        }
    }

    /** Undoes the {@code player}'s last click; null when there is nothing to undo. */
    @Nullable
    public static Result undo(Player player, ItemStack held) {
        Deque<Step> steps = HISTORY.get(player.getUUID());
        Step step = steps != null ? steps.poll() : null;
        if (step == null) {
            return null;
        }
        Level level = player.level().dimension() == step.dimension() ? player.level()
                : player.getServer() != null ? player.getServer().getLevel(step.dimension()) : null;
        int restored = 0;
        if (level != null) {
            for (Change change : step.changes()) {
                if (level.isLoaded(change.pos()) && DecalEdits.mayEdit(player, level, change.pos(), change.face(), held)
                        && DecalHolder.at(level, change.pos()) instanceof DecalHolder be
                        && Objects.equals(be.getDecal(change.face(), change.front()), change.after())) {
                    be.setDecal(change.face(), change.front(), change.before());
                    restored++;
                }
            }
            if (restored > 0) {
                AllSoundEvents.CLIPBOARD_ERASE.playOnServer(level, step.changes().get(0).pos(), 1, 0.8f);
            }
        }
        return new Result(restored, step.changes().size());
    }

    /** How many clicks the {@code player} can undo (for tests). */
    public static int steps(Player player) {
        Deque<Step> steps = HISTORY.get(player.getUUID());
        return steps != null ? steps.size() : 0;
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        HISTORY.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HISTORY.clear();
    }
}
