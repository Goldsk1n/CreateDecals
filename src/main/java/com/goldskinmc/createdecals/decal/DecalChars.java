package com.goldskinmc.createdecals.decal;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * Which characters can be typed as decals, and the built-in decal (its path, in our namespace) for each: letters, digits
 * and signs. Pure Java, so unit tests can check every one has a sprite; {@link DecalText} turns them into ids.
 */
public final class DecalChars {
    private static final String SIGNS = "&-.#+/!?:'()";
    private static final String[] SIGN_PATHS = {"sign_and", "sign_dash", "sign_dot", "sign_hash", "sign_plus", "sign_slash",
            "sign_exclaim", "sign_question", "sign_colon", "sign_apostrophe", "sign_paren_left", "sign_paren_right"};

    private DecalChars() {
    }

    /** Whether {@code c} can be typed (either case; a space leaves a gap). */
    public static boolean allowed(char c) {
        char upper = Character.toUpperCase(c);
        return upper == ' ' || (upper >= 'A' && upper <= 'Z') || (upper >= '0' && upper <= '9') || SIGNS.indexOf(upper) >= 0;
    }

    /** Upper case, only allowed characters, at most {@code max}. */
    public static String normalise(String text, int max) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toUpperCase(Locale.ROOT).toCharArray()) {
            if (allowed(c) && out.length() < max) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** The decal's path for one character, or null for a space (or anything not allowed). */
    @Nullable
    public static String pathFor(char c) {
        char upper = Character.toUpperCase(c);
        if (upper >= 'A' && upper <= 'Z') {
            return "letter_" + Character.toLowerCase(upper);
        }
        if (upper >= '0' && upper <= '9') {
            return "digit_" + upper;
        }
        int sign = SIGNS.indexOf(upper);
        return sign >= 0 ? SIGN_PATHS[sign] : null;
    }
}
