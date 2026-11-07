package com.goldskinmc.createdecals.decal;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * A typed word as decals: one built-in letter, digit or sign per block, spaces left empty. Turned and flipped as a
 * whole, like a decal {@code length} x 1 tiles wide ({@link DecalPlacement#cellOf(int, int, int, boolean, int, int)}).
 */
public final class DecalText {
    /** Longest word, in blocks. */
    public static final int MAX_LENGTH = 16;
    private static final String SIGNS = "&-.#+/";
    private static final String[] SIGN_IDS = {"sign_and", "sign_dash", "sign_dot", "sign_hash", "sign_plus", "sign_slash"};

    private DecalText() {
    }

    /** Whether {@code c} can be typed (either case; a space leaves a gap). */
    public static boolean allowed(char c) {
        char upper = Character.toUpperCase(c);
        return upper == ' ' || (upper >= 'A' && upper <= 'Z') || (upper >= '0' && upper <= '9') || SIGNS.indexOf(upper) >= 0;
    }

    /** Upper case, only allowed characters, at most {@link #MAX_LENGTH}. */
    public static String normalise(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toUpperCase(Locale.ROOT).toCharArray()) {
            if (allowed(c) && out.length() < MAX_LENGTH) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** The decal for one character, or null for a space (or anything not allowed). */
    @Nullable
    public static ResourceLocation decalFor(char c) {
        char upper = Character.toUpperCase(c);
        if (upper >= 'A' && upper <= 'Z') {
            return Decals.id("letter_" + Character.toLowerCase(upper));
        }
        if (upper >= '0' && upper <= '9') {
            return Decals.id("digit_" + upper);
        }
        int sign = SIGNS.indexOf(upper);
        return sign >= 0 ? Decals.id(SIGN_IDS[sign]) : null;
    }
}
