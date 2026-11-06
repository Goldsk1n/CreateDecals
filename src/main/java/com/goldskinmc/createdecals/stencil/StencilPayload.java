package com.goldskinmc.createdecals.stencil;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the stencil screen's choice, to be stored on the stencil in that hand. */
public record StencilPayload(InteractionHand hand, StencilData data) implements CustomPacketPayload {
    public static final Type<StencilPayload> TYPE = new Type<>(CreateDecals.id("stencil"));
    public static final StreamCodec<ByteBuf, StencilPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.map(main -> main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                    hand -> hand == InteractionHand.MAIN_HAND), StencilPayload::hand,
            StencilData.STREAM_CODEC, StencilPayload::data,
            StencilPayload::new);

    @Override
    public Type<StencilPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, StencilPayload::handle);
    }

    /** Only onto a stencil the sender holds in that hand. */
    private static void handle(StencilPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getItemInHand(payload.hand());
            if (stack.getItem() instanceof DecalStencilItem) {
                stack.set(DecalsComponents.STENCIL.get(), payload.data());
            }
        });
    }
}
