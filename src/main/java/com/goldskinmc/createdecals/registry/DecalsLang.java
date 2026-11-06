package com.goldskinmc.createdecals.registry;

import static com.goldskinmc.createdecals.CreateDecals.REGISTRATE;

import com.goldskinmc.createdecals.CreateDecals;

/**
 * English text that is not a block or item name: tooltips (Create's "Hold Shift" summary), the stencil's screen and
 * messages, the creative tab.
 */
public final class DecalsLang {
    private DecalsLang() {
    }

    public static void register() {
        add("itemGroup." + CreateDecals.MOD_ID, "Create: Decals");
        tooltip("item", "decal_stencil", "Puts _decals_ on Decal Panels and Decal Blocks. Choose the decal, its colour and how it turns in its screen.",
                action("When used in the air", "Opens the stencil's screen."),
                action("When R-clicked on a face", "Puts the decal there, or erases it."),
                action("When R-clicked while sneaking", "Picks up the decal on that face."));
        add("createdecals.stencil.title", "Decal Stencil");
        add("createdecals.stencil.rotate", "Turn");
        add("createdecals.stencil.flip", "Flip");
        add("createdecals.stencil.erase", "Eraser");
        add("createdecals.stencil.done", "Done");
        add("createdecals.stencil.colour", "Colour");
        add("createdecals.stencil.front_only", "A Decal Panel takes a decal on its front only");
        add("createdecals.stencil.no_decal", "Choose a decal first: use the stencil in the air");
        add("createdecals.stencil.nothing_to_pick", "No decal on that face");
        add("createdecals.stencil.picked", "Decal picked up");
        add("createdecals.stencil.partly", "Only %s of its %s tiles found a decal block face");
        add("createdecals.stencil.tooltip.empty", "No decal chosen");
        add("createdecals.stencil.tooltip.erase", "Eraser");
        add("createdecals.stencil.tooltip.decal", "%s, %s");
        for (String category : new String[] {"letters", "digits", "signs", "lines", "markers", "emblems", "misc"}) {
            add("createdecals.category." + category, Character.toUpperCase(category.charAt(0)) + category.substring(1));
        }
        tooltip("block", "decal_panel", "A _copycat panel_ that shows a _decal_ on its front: stripes, numbers, letters, emblems. Keeps it on contraptions, trains and Sable vehicles.",
                behaviour("When R-clicked with a block", "Copies that block's look, as Create's Copycat Panel does."),
                action("When R-clicked with a Decal Stencil", "Puts the stencil's decal on its front."),
                action("When R-clicked with Dye", "Recolours its decal."));
        tooltip("block", "decal_block", "A full _copycat block_ with a _decal_ on any of its faces, for hulls built of whole blocks.",
                behaviour("When R-clicked with a block", "Copies that block's look."),
                action("When R-clicked with a Decal Stencil", "Puts the stencil's decal on the clicked face."),
                action("When R-clicked with Dye", "Recolours the decal on that face."));
    }

    /**
     * Keys read by Create's {@code ItemDescription}: {@code <block key>.tooltip.summary}, then numbered condition and
     * behaviour pairs, then numbered control and action pairs. Words in _underscores_ are highlighted.
     */
    private static void tooltip(String kind, String name, String summary, Line... lines) {
        String key = kind + "." + CreateDecals.MOD_ID + "." + name + ".tooltip";
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
