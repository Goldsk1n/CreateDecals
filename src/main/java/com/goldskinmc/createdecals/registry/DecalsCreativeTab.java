package com.goldskinmc.createdecals.registry;

import com.goldskinmc.createdecals.CreateDecals;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Our own creative tab, placed after Create's, as Lightworks does. */
public final class DecalsCreativeTab {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateDecals.MOD_ID);
    private static final ResourceKey<CreativeModeTab> CREATE_PALETTES =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("create", "palettes"));

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + CreateDecals.MOD_ID))
                .icon(DecalsBlocks.DECAL_PANEL::asStack)
                .withTabsBefore(CREATE_PALETTES)
                .displayItems((parameters, output) -> contents().forEach(output::accept))
                .build());
    }

    private DecalsCreativeTab() {
    }

    public static List<ItemLike> contents() {
        return List.of(DecalsItems.DECAL_STENCIL, DecalsBlocks.DECAL_PANEL, DecalsBlocks.DECAL_BLOCK);
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
