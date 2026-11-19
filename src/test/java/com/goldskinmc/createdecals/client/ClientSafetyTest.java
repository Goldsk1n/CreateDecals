package com.goldskinmc.createdecals.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** An effect that throws falls back, turns itself off for the session, and leaves the other effects alone. */
class ClientSafetyTest {
    private final List<String> reported = new ArrayList<>();

    @BeforeEach
    void listen() {
        ClientSafety.report = (effect, cause) -> reported.add(effect);
    }

    @AfterEach
    void reset() {
        ClientSafety.reset();
    }

    @Test
    void workingEffectGivesItsResult() {
        assertEquals("decal", ClientSafety.run(ClientSafety.DECALS, () -> "decal", "plain"));
        assertTrue(ClientSafety.works(ClientSafety.DECALS));
    }

    @Test
    void failingEffectFallsBackAndStaysOff() {
        AtomicInteger calls = new AtomicInteger();
        String first = ClientSafety.run(ClientSafety.DECALS, () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("another mod's model");
        }, "plain");
        assertEquals("plain", first);
        assertFalse(ClientSafety.works(ClientSafety.DECALS));
        // Not tried again until the game restarts
        assertEquals("plain", ClientSafety.run(ClientSafety.DECALS, () -> {
            calls.incrementAndGet();
            return "decal";
        }, "plain"));
        assertEquals(1, calls.get());
        assertEquals(List.of(ClientSafety.DECALS), reported);
    }

    @Test
    void otherEffectsCarryOn() {
        ClientSafety.run(ClientSafety.PREVIEW, () -> {
            throw new IllegalStateException("boom");
        });
        assertFalse(ClientSafety.works(ClientSafety.PREVIEW));
        assertTrue(ClientSafety.works(ClientSafety.ICON));
        assertEquals("icon", ClientSafety.run(ClientSafety.ICON, () -> "icon", "plain"));
    }
}
