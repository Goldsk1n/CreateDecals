package com.goldskinmc.createdecals.client;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * What to do when our drawing goes wrong next to some other mod, as in Create: Lightworks: an effect that fails turns
 * itself off for the session and says so once in the log, rather than taking the player's game down. The game carries
 * on as it would without that effect (copycats keep their material, the stencil its plain icon). Any thread: block
 * models are built on chunk workers.
 */
public final class ClientSafety {
    /** Decals drawn on blocks: our Decal Block's and Create's and Copycats+'s copycats' models. */
    public static final String DECALS = "decals on blocks";
    /** Glowing decals drawn full-bright again on contraptions. */
    public static final String GLOW = "glowing decals on contraptions";
    /** The held stencil's ghost and outlines. */
    public static final String PREVIEW = "stencil preview";
    /** The stencil's icon showing its decal. */
    public static final String ICON = "stencil icons";

    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
    /** Where a failure is told, once per effect: the game's log (tests listen instead). */
    static BiConsumer<String, RuntimeException> report = ClientSafety::log;

    private ClientSafety() {
    }

    public static boolean works(String effect) {
        return !FAILED.contains(effect);
    }

    public static void failed(String effect, RuntimeException cause) {
        if (FAILED.add(effect)) {
            report.accept(effect, cause);
        }
    }

    private static void log(String effect, RuntimeException cause) {
        LogUtils.getLogger().error("Create: Decals turned off its {} until the game restarts: they failed, likely next to "
                + "another mod. Please report this with your mod list.", effect, cause);
    }

    /** {@code action}'s result, or {@code fallback} once {@code effect} has failed (now or before). */
    public static <T> T run(String effect, Supplier<T> action, T fallback) {
        if (!works(effect)) {
            return fallback;
        }
        try {
            return action.get();
        } catch (RuntimeException e) {
            failed(effect, e);
            return fallback;
        }
    }

    /** Runs {@code action} unless {@code effect} has failed; a failure turns it off. */
    public static void run(String effect, Runnable action) {
        run(effect, () -> {
            action.run();
            return null;
        }, null);
    }

    /** For tests: every effect on again, failures told to the log. */
    static void reset() {
        FAILED.clear();
        report = ClientSafety::log;
    }
}
