package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Opens the stencil's screen when it is used in the air (common code never refers to client classes). */
@EventBusSubscriber(modid = CreateDecals.MOD_ID, value = Dist.CLIENT)
public final class StencilClientEvents {
    private StencilClientEvents() {
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() && event.getItemStack().getItem() instanceof DecalStencilItem
                && event.getEntity() == Minecraft.getInstance().player) {
            Minecraft.getInstance().setScreen(new StencilScreen(event.getHand(), event.getItemStack()));
        }
    }
}
