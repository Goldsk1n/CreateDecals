package com.goldskinmc.createdecals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.goldskinmc.createdecals.decal.DecalChars;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Typed words: every character that can be typed has a built-in decal, drawn and defined with the mod. */
class DecalTextTest {
    private static final Path DECALS = Path.of("src/main/resources/assets/createdecals");
    private static final String TYPEABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789&-.#+/!?:'()";

    @Test
    void everyTypeableCharacterHasADecal() {
        for (char c : TYPEABLE.toCharArray()) {
            assertTrue(DecalChars.allowed(c), "should be typeable: " + c);
            String path = DecalChars.pathFor(c);
            assertNotNull(path, "no decal for " + c);
            assertTrue(Files.exists(DECALS.resolve("textures/decal/" + path + ".png")), "no sprite for " + c + ": " + path);
            assertTrue(Files.exists(DECALS.resolve("decals/" + path + ".json")), "no definition for " + c + ": " + path);
        }
    }

    @Test
    void wordsAreUpperCaseAndLeaveOutTheRest() {
        assertEquals("NO.7: GO!", DecalChars.normalise("no.7: go!", 16));
        assertEquals("DON'T (YET)?", DecalChars.normalise("don't (yet)?", 16));
        assertEquals("AB", DecalChars.normalise("a~b", 16));
        assertNull(DecalChars.pathFor(' '));
        assertFalse(DecalChars.allowed('~'));
    }
}
