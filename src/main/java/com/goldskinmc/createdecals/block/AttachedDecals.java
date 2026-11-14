package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalState;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

/**
 * The decals on another mod's copycat (Copycats+), kept as a NeoForge data attachment on its block entity: back and front
 * layers by face, saved as our own blocks save them ("Decals", "FrontDecals"). An attachment is saved and loaded with
 * the block entity, so it rides contraptions, schematics and Sable vehicles with it; clients get it from
 * {@link DecalSync}, since NeoForge 21.1 does not send block entity attachments.
 */
public record AttachedDecals(Map<Direction, DecalState> back, Map<Direction, DecalState> front) {
    public static final AttachedDecals EMPTY = new AttachedDecals(Map.of(), Map.of());

    public AttachedDecals {
        back = Collections.unmodifiableMap(copy(back));
        front = Collections.unmodifiableMap(copy(front));
    }

    private static EnumMap<Direction, DecalState> copy(Map<Direction, DecalState> layer) {
        return layer.isEmpty() ? new EnumMap<>(Direction.class) : new EnumMap<>(layer);
    }

    public boolean isEmpty() {
        return back.isEmpty() && front.isEmpty();
    }

    @Nullable
    public DecalState get(Direction face, boolean front) {
        return (front ? this.front : back).get(face);
    }

    public AttachedDecals with(Direction face, boolean front, @Nullable DecalState decal) {
        EnumMap<Direction, DecalState> layer = copy(front ? this.front : back);
        if (decal == null) {
            layer.remove(face);
        } else {
            layer.put(face, decal);
        }
        return front ? new AttachedDecals(back, layer) : new AttachedDecals(layer, this.front);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Decals", saveLayer(back));
        tag.put("FrontDecals", saveLayer(front));
        return tag;
    }

    public static AttachedDecals load(CompoundTag tag) {
        return new AttachedDecals(loadLayer(tag.getCompound("Decals")), loadLayer(tag.getCompound("FrontDecals")));
    }

    private static CompoundTag saveLayer(Map<Direction, DecalState> layer) {
        CompoundTag tag = new CompoundTag();
        layer.forEach((face, decal) -> tag.put(face.getSerializedName(), decal.save()));
        return tag;
    }

    private static Map<Direction, DecalState> loadLayer(CompoundTag tag) {
        EnumMap<Direction, DecalState> layer = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            if (tag.contains(face.getSerializedName())) {
                DecalState decal = DecalState.load(tag.getCompound(face.getSerializedName()));
                if (decal != null) {
                    layer.put(face, decal);
                }
            }
        }
        return layer;
    }

    /**
     * Saves only when there are decals. Reading one on the server (a chunk loading, a contraption putting its blocks back,
     * a schematic) asks {@link DecalSync} to send it to the players who see that block.
     */
    public static final IAttachmentSerializer<CompoundTag, AttachedDecals> SERIALIZER = new IAttachmentSerializer<>() {
        @Override
        public AttachedDecals read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            AttachedDecals decals = load(tag);
            if (holder instanceof BlockEntity be && !decals.isEmpty()) {
                DecalSync.loaded(be);
            }
            return decals;
        }

        @Override
        @Nullable
        public CompoundTag write(AttachedDecals attachment, HolderLookup.Provider provider) {
            return attachment.isEmpty() ? null : attachment.save();
        }
    };
}
