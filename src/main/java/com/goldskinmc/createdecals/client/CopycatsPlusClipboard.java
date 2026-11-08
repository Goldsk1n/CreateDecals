package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalClipboard;
import com.goldskinmc.createdecals.compat.CopycatsPlus;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Create's Clipboard outlines the block under the crosshair and says what a click would copy or paste, on the client,
 * before any click: a Copycats+ copycat aimed at with a Clipboard in hand gets our clipboard behaviour there first
 * ({@link DecalClipboard}).
 */
@EventBusSubscriber(modid = CreateDecals.MOD_ID, value = Dist.CLIENT)
public final class CopycatsPlusClipboard {
    private CopycatsPlusClipboard() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!CopycatsPlus.loaded() || mc.player == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockEntity be = mc.level.getBlockEntity(hit.getBlockPos());
        if (be != null) {
            DecalClipboard.attachIfHeld(mc.player.getMainHandItem(), be);
        }
    }
}
