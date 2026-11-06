package com.goldskinmc.createdecals.registry;

import static com.goldskinmc.createdecals.CreateDecals.REGISTRATE;

import com.goldskinmc.createdecals.CreateDecals;

/** English text that is not a block name: item tooltips (Create's "Hold Shift" summary) and the creative tab. */
public final class DecalsLang {
    private DecalsLang() {
    }

    public static void register() {
        add("itemGroup." + CreateDecals.MOD_ID, "Create: Decals");
        tooltip("decal_panel", "A _copycat panel_ that shows a _decal_ on its front: stripes, numbers, letters, emblems. Keeps it on contraptions, trains and Sable vehicles.",
                behaviour("When R-clicked with a block", "Copies that block's look, as Create's Copycat Panel does."),
                action("When R-clicked with Dye", "Recolours its decal."));
        tooltip("decal_block", "A full _copycat block_ with a _decal_ on any of its faces, for hulls built of whole blocks.",
                behaviour("When R-clicked with a block", "Copies that block's look."),
                action("When R-clicked with Dye", "Recolours the decal on that face."));
    }

    /**
     * Keys read by Create's {@code ItemDescription}: {@code <block key>.tooltip.summary}, then numbered condition and
     * behaviour pairs, then numbered control and action pairs. Words in _underscores_ are highlighted.
     */
    private static void tooltip(String block, String summary, Line... lines) {
        String key = "block." + CreateDecals.MOD_ID + "." + block + ".tooltip";
        add(key + ".summary", summary);
        int behaviours = 0;
        int actions = 0;
        for (Line line : lines) {
            if (line.action()) {
                actions++;
                add(key + ".control" + actions, line.condition());
                add(key + ".action" + actions, line.text());
            } else {
                behaviours++;
                add(key + ".condition" + behaviours, line.condition());
                add(key + ".behaviour" + behaviours, line.text());
            }
        }
    }

    private static Line behaviour(String condition, String text) {
        return new Line(condition, text, false);
    }

    private static Line action(String control, String text) {
        return new Line(control, text, true);
    }

    private record Line(String condition, String text, boolean action) {
    }

    private static void add(String key, String text) {
        REGISTRATE.addRawLang(key, text);
    }
}
