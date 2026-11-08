package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.CreateCopycats;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
            // Used on a block that takes no decals (the stencil passes such clicks on to using it in the air): the screen
            // opens anyway, which is handy where nothing in reach is open air, and says where decals go
            Minecraft mc = Minecraft.getInstance();
            Component wrongBlock = null;
            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && mc.level != null) {
                BlockState state = mc.level.getBlockState(hit.getBlockPos());
                if (!(mc.level.getBlockEntity(hit.getBlockPos()) instanceof DecalCopycatBlockEntity) && !CreateCopycats.isCreateCopycat(state)) {
                    wrongBlock = state.getBlock().getName();
                }
            }
            mc.setScreen(new StencilScreen(event.getHand(), event.getItemStack(), wrongBlock));
        }
    }
}
