package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.goldskinmc.createdecals.stencil.StencilPayload;
import com.goldskinmc.createdecals.stencil.UndoPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The stencil's shortcuts in the world, while it is held: Ctrl + scroll turns its decal or word a quarter (Create's own
 * habit for held tools, as its schematic tools do), and the undo key (Z by default, under Controls) undoes the last
 * stencil click ({@code block/DecalUndo}). Both work only while a stencil is in hand and no screen is open.
 */
@EventBusSubscriber(modid = CreateDecals.MOD_ID, value = Dist.CLIENT)
public final class StencilKeys {
    /** Active only while a stencil is held in the world; it shares its key with in-game bindings, so Controls flags a clash. */
    private static final IKeyConflictContext HOLDING_STENCIL = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            Minecraft mc = Minecraft.getInstance();
            return mc.screen == null && mc.player != null && heldHand(mc.player) != null;
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return other == this || other == KeyConflictContext.IN_GAME;
        }
    };

    public static final KeyMapping UNDO = new KeyMapping("key.createdecals.undo", HOLDING_STENCIL, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z, "key.categories.createdecals");

    private StencilKeys() {
    }

    @EventBusSubscriber(modid = CreateDecals.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(UNDO);
        }
    }

    /** The hand holding a stencil (the main hand first), or null. */
    @Nullable
    static InteractionHand heldHand(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof DecalStencilItem) {
                return hand;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || !Screen.hasControlDown() || event.getScrollDeltaY() == 0) {
            return;
        }
        InteractionHand hand = heldHand(mc.player);
        if (hand != null && turn(mc.player, hand, event.getScrollDeltaY() > 0 ? 1 : -1)) {
            event.setCanceled(true);
        }
    }

    /**
     * Turns the decal or word on the stencil in {@code hand} a quarter clockwise ({@code by} 1) or back (-1): on the
     * client at once, so the ghost turns with it, and on the server through the screen's own payload. False when the
     * stencil holds nothing to turn (empty, or the eraser).
     */
    public static boolean turn(LocalPlayer player, InteractionHand hand, int by) {
        ItemStack stack = player.getItemInHand(hand);
        StencilData data = DecalStencilItem.data(stack);
        if (data.erase() || (!data.writes() && data.decal().isEmpty())) {
            return false;
        }
        StencilData turned = data.withTurn(data.rotation() + by, data.flip());
        stack.set(DecalsComponents.STENCIL.get(), turned);
        PacketDistributor.sendToServer(new StencilPayload(hand, turned));
        player.displayClientMessage(Component.translatable("createdecals.stencil.turned", turned.rotation() * 90), true);
        return true;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (UNDO.consumeClick()) {
            if (mc.screen == null && mc.player != null && heldHand(mc.player) != null) {
                PacketDistributor.sendToServer(UndoPayload.INSTANCE);
            }
        }
    }
}
