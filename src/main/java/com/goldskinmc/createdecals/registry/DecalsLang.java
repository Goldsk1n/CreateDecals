package com.goldskinmc.createdecals.registry;

import static com.goldskinmc.createdecals.CreateDecals.REGISTRATE;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.ponder.DecalsPonder;
import com.tterrag.registrate.providers.ProviderType;

/**
 * English text that is not a block or item name: tooltips (Create's "Hold Shift" summary), the stencil's screen and
 * messages, the creative tab.
 */
public final class DecalsLang {
    private DecalsLang() {
    }

    public static void register() {
        add("itemGroup." + CreateDecals.MOD_ID, "Create: Decals");
        // Ponder scene text, collected from the scenes themselves
        REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> DecalsPonder.provideLang(provider::add));
        tooltip("item", "decal_stencil", "Puts _decals_ on copycat blocks.",
                behaviour("When a decal is R-clicked with Dye", "Recolours it. A _Glow Ink Sac_ makes it glow."),
                action("When used in the air", "Opens its screen."),
                action("When R-clicked on a face", "Puts the decal on, or erases it."),
                action("When R-clicked while sneaking", "Picks the decal up."),
                action("When scrolled holding Ctrl", "Turns the decal."),
                action("When Undo (Z) is pressed", "Undoes the last click."));
        add("createdecals.stencil.title", "Decal Stencil");
        add("createdecals.stencil.rotate", "Turn");
        add("createdecals.stencil.flip", "Flip");
        add("createdecals.stencil.erase", "Eraser");
        add("createdecals.stencil.done", "Done");
        add("createdecals.stencil.colour", "Colour");
        add("createdecals.stencil.wrong_block", "%s takes no decals");
        add("createdecals.stencil.turned", "Turned to %s°");
        add("createdecals.stencil.undone", "Undone: %s tiles");
        add("createdecals.stencil.undone_partly", "Undone: %s of %s tiles");
        add("createdecals.stencil.undo_none", "Nothing to undo");
        add("key.createdecals.undo", "Undo last stencil click");
        add("key.categories.createdecals", "Create: Decals");
        add("createdecals.stencil.no_decal", "Choose a decal first: use the stencil in the air");
        add("createdecals.stencil.nothing_to_pick", "No decal on that face");
        add("createdecals.stencil.picked", "Decal picked up");
        add("createdecals.stencil.picked_front", "Front decal picked up");
        add("createdecals.stencil.layer_back", "Back layer: underneath");
        add("createdecals.stencil.layer_front", "Front layer: on top");
        add("createdecals.stencil.flip_words", "Words are not mirrored");
        add("createdecals.stencil.glow_on", "Glows in the dark");
        add("createdecals.stencil.glow_off", "Not glowing");
        add("createdecals.stencil.text", "Word");
        add("createdecals.stencil.text_hint", "Type a word...");
        add("createdecals.stencil.tooltip.text", "\"%s\", %s");
        add("createdecals.stencil.tooltip.front", "Front layer");
        add("createdecals.stencil.tooltip.glow", "Glowing");
        add("createdecals.stencil.partly", "Only %s of %s tiles fit");
        add("createdecals.stencil.tooltip.empty", "No decal chosen");
        add("createdecals.stencil.tooltip.erase", "Eraser");
        add("createdecals.stencil.tooltip.decal", "%s, %s");
        // Decals whose names are not their ids in words (the screen makes the rest from the id)
        String[][] names = {
                {"sign_and", "Ampersand"}, {"sign_dash", "Dash"}, {"sign_dot", "Full Stop"}, {"sign_hash", "Hash"},
                {"sign_plus", "Plus"}, {"sign_slash", "Slash"}, {"sign_exclaim", "Exclamation Mark"},
                {"sign_question", "Question Mark"}, {"sign_colon", "Colon"}, {"sign_apostrophe", "Apostrophe"},
                {"sign_paren_left", "Opening Bracket"}, {"sign_paren_right", "Closing Bracket"}, {"no_entry", "No Entry"},
                {"drop", "Water Drop"}, {"skull_crossbones", "Skull and Crossbones"}};
        for (String[] name : names) {
            add("decal.createdecals." + name[0], name[1]);
        }
        for (String category : new String[] {"letters", "digits", "signs", "lines", "markers", "emblems", "misc"}) {
            add("createdecals.category." + category, Character.toUpperCase(category.charAt(0)) + category.substring(1));
        }
        tooltip("block", "decal_block", "A full _copycat block_ that takes a _decal_ on each face.",
                behaviour("When R-clicked with a block", "Copies its look."));
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
