package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.decal.DecalState;
import java.util.EnumMap;
import java.util.Map;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;

/**
 * Glowing decals in Ponder scenes. A glowing decal is baked unshaded at full light, so it looks the same on every face,
 * also at night. Ponder shades its scenes itself and gives every unshaded quad an upward face's brightness, which in its
 * view is darker than the front: a glowing decal there looked dimmer than before it glowed. Ponder has no darkness, so a
 * glow cannot show there anyway; its scenes get their decals as ordinary ones.
 */
final class PonderGlow {
    private PonderGlow() {
    }

    static boolean inPonder(BlockAndTintGetter level) {
        return level instanceof PonderLevel;
    }

    /** {@code decals} with none glowing; null stays null. */
    @Nullable
    static Map<Direction, DecalState> unlit(@Nullable Map<Direction, DecalState> decals) {
        if (decals == null || decals.values().stream().noneMatch(DecalState::glow)) {
            return decals;
        }
        Map<Direction, DecalState> out = new EnumMap<>(Direction.class);
        decals.forEach((face, decal) -> out.put(face, decal.withGlow(false)));
        return Map.copyOf(out);
    }
}
