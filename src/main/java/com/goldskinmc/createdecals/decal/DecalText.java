package com.goldskinmc.createdecals.decal;

import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * A typed word as decals: one built-in letter, digit or sign per block, spaces left empty. Turned and flipped as a
 * whole, like a decal {@code length} x 1 tiles wide ({@link DecalPlacement#cellOf(int, int, int, boolean, int, int)}).
 */
public final class DecalText {
    /** Longest word, in blocks. */
    public static final int MAX_LENGTH = 16;

    private DecalText() {
    }

    /** Whether {@code c} can be typed (either case; a space leaves a gap). */
    public static boolean allowed(char c) {
        return DecalChars.allowed(c);
    }

    /** Upper case, only allowed characters, at most {@link #MAX_LENGTH}. */
    public static String normalise(String text) {
        return DecalChars.normalise(text, MAX_LENGTH);
    }

    /** The decal for one character, or null for a space (or anything not allowed). */
    @Nullable
    public static ResourceLocation decalFor(char c) {
        String path = DecalChars.pathFor(c);
        return path != null ? Decals.id(path) : null;
    }
}
