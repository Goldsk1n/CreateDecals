package com.goldskinmc.createdecals.stencil;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalUndo;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the undo key was pressed while holding a stencil; undoes the sender's last stencil click. */
public record UndoPayload() implements CustomPacketPayload {
    public static final UndoPayload INSTANCE = new UndoPayload();
    public static final Type<UndoPayload> TYPE = new Type<>(CreateDecals.id("undo"));
    public static final StreamCodec<ByteBuf, UndoPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<UndoPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, UndoPayload::handle);
    }

    /** Only while the sender holds a stencil, as the key works only then. */
    private static void handle(UndoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack held = ItemStack.EMPTY;
            for (InteractionHand hand : InteractionHand.values()) {
                if (player.getItemInHand(hand).getItem() instanceof DecalStencilItem) {
                    held = player.getItemInHand(hand);
                    break;
                }
            }
            if (!held.isEmpty()) {
                player.displayClientMessage(undo(player, held), true);
            }
        });
    }

    /** Undoes the player's last stencil click and says what came of it. */
    public static Component undo(Player player, ItemStack held) {
        DecalUndo.Result result = DecalUndo.undo(player, held);
        if (result == null) {
            return Component.translatable("createdecals.stencil.undo_none");
        }
        return result.restored() == result.total() ? Component.translatable("createdecals.stencil.undone", result.total())
                : Component.translatable("createdecals.stencil.undone_partly", result.restored(), result.total());
    }
}
