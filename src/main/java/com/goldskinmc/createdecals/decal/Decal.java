package com.goldskinmc.createdecals.decal;

import com.goldskinmc.createdecals.CreateDecals;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The fixed decal library. Each decal is one white 16x16 sprite on the block atlas
 * ({@code assets/createdecals/textures/block/decal/<name>.png}), tinted with the
 * panel's colour when it is baked into the panel's model.
 */
public enum Decal {
    /** The whole face: a solid colour, and the reference target for the z-fighting measurements. */
    FILL,
    STRIPE,
    STRIPE_THIN,
    /** A band along the bottom edge, so a row of panels draws one continuous line. */
    STRIPE_EDGE,
    HAZARD,
    CHEVRON,
    DIGIT_0, DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4, DIGIT_5, DIGIT_6, DIGIT_7, DIGIT_8, DIGIT_9,
    LETTER_A, LETTER_B, LETTER_C, LETTER_E, LETTER_K, LETTER_L, LETTER_N, LETTER_R, LETTER_S, LETTER_X,
    /** A cog, in the spirit of Create's own logo. */
    EMBLEM;

    private static final Decal[] VALUES = values();

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(CreateDecals.MOD_ID, "block/decal/" + id());
    }

    public static Decal byIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }

    @Nullable
    public static Decal byId(String id) {
        for (Decal decal : VALUES) {
            if (decal.id().equals(id)) {
                return decal;
            }
        }
        return null;
    }
}
