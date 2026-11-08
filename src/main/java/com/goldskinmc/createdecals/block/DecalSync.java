package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.registry.DecalsAttachments;
import io.netty.buffer.ByteBuf;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sends the decals on other mods' copycats ({@link AttachedDecals}) to clients, which NeoForge 21.1 does not do for block
 * entity attachments: when they change, with every chunk a player starts to see, and when a block entity carrying them
 * is loaded on the server (a contraption putting its blocks back, a schematic), a tick later, once it is in its level.
 */
@EventBusSubscriber(modid = CreateDecals.MOD_ID)
public final class DecalSync {
    private static final Queue<BlockEntity> LOADED = new ConcurrentLinkedQueue<>();

    private DecalSync() {
    }

    /** Server to client: all decals of the copycat at {@code pos} (an empty tag: none). */
    public record Payload(BlockPos pos, CompoundTag decals) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(CreateDecals.id("copycat_decals"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Payload::pos,
                ByteBufCodecs.COMPOUND_TAG, Payload::decals,
                Payload::new);

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Payload.TYPE, Payload.STREAM_CODEC, DecalSync::receive);
    }

    private static Payload payloadOf(BlockEntity be) {
        AttachedDecals decals = be.getExistingData(DecalsAttachments.DECALS.get()).orElse(AttachedDecals.EMPTY);
        return new Payload(be.getBlockPos(), decals.isEmpty() ? new CompoundTag() : decals.save());
    }

    /** To every player who sees the block. */
    static void send(BlockEntity be) {
        if (be.getLevel() instanceof ServerLevel level) {
            PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(be.getBlockPos()), payloadOf(be));
        }
    }

    /** A block entity with decals was read on the server; sent next tick (it may not be in a level yet). */
    static void loaded(BlockEntity be) {
        if (be.getLevel() == null || !be.getLevel().isClientSide()) {
            LOADED.add(be);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        BlockEntity be;
        while ((be = LOADED.poll()) != null) {
            Level level = be.getLevel();
            if (level instanceof ServerLevel && !be.isRemoved() && level.getBlockEntity(be.getBlockPos()) == be) {
                send(be);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        for (BlockEntity be : event.getChunk().getBlockEntities().values()) {
            if (be.hasData(DecalsAttachments.DECALS.get())) {
                PacketDistributor.sendToPlayer(event.getPlayer(), payloadOf(be));
            }
        }
    }

    private static void receive(Payload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity be = context.player().level().getBlockEntity(payload.pos());
            if (be != null) {
                AttachedDecalHolder.set(be, payload.decals().isEmpty() ? AttachedDecals.EMPTY : AttachedDecals.load(payload.decals()));
            }
        });
    }
}
