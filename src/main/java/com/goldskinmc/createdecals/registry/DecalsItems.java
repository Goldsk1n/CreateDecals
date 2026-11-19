package com.goldskinmc.createdecals.registry;

import static com.goldskinmc.createdecals.CreateDecals.REGISTRATE;

import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.tterrag.registrate.util.entry.ItemEntry;

/** The Decal Stencil; its icon is two layers, the second tinted with the chosen colour (client/DecalsClient). */
public final class DecalsItems {
    public static final ItemEntry<DecalStencilItem> DECAL_STENCIL = REGISTRATE
            .item("decal_stencil", DecalStencilItem::new)
            .properties(p -> p.stacksTo(1))
            .model((c, p) -> {
                p.generated(c, p.modLoc("item/decal_stencil"), p.modLoc("item/decal_stencil_mark"));
                // Shown instead while the stencil is the eraser (client/StencilItemModel)
                p.withExistingParent("decal_stencil_eraser", p.mcLoc("item/generated"))
                        .texture("layer0", p.modLoc("item/decal_stencil_eraser"));
            })
            .lang("Decal Stencil")
            .register();

    private DecalsItems() {
    }

    public static void register() {
        // Loading this class registers the entries above
    }
}
