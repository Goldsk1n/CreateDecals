package com.goldskinmc.createdecals;

import com.goldskinmc.createdecals.command.DecalsCommand;
import com.goldskinmc.createdecals.registry.DecalsBlockEntities;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.goldskinmc.createdecals.registry.DecalsCreativeTab;
import com.goldskinmc.createdecals.registry.DecalsLang;
import com.goldskinmc.createdecals.registry.DecalsRecipes;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(CreateDecals.MOD_ID)
public final class CreateDecals {
    public static final String MOD_ID = "createdecals";
    public static final Logger LOGGER = LogUtils.getLogger();
    // No default tab: DecalsCreativeTab lists our items itself (Registrate would add them a second time)
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
            .setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));

    public CreateDecals(IEventBus modBus) {
        REGISTRATE.registerEventListeners(modBus);
        DecalsBlocks.register();
        DecalsBlockEntities.register();
        DecalsRecipes.register();
        DecalsCreativeTab.register(modBus);
        DecalsLang.register();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> DecalsCommand.register(event.getDispatcher()));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
