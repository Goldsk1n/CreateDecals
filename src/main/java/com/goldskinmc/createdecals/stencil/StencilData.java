package com.goldskinmc.createdecals.stencil;

import com.goldskinmc.createdecals.decal.DecalState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * What a Decal Stencil holds, as a data component on the item: the chosen decal (none yet on a new stencil), its colour,
 * quarter turns and flip, or the eraser.
 */
public record StencilData(Optional<ResourceLocation> decal, int rgb, int rotation, boolean flip, boolean erase) {
    public static final int WHITE = DyeColor.WHITE.getTextureDiffuseColor() & 0xFFFFFF;
    public static final StencilData EMPTY = new StencilData(Optional.empty(), WHITE, 0, false, false);

    public static final Codec<StencilData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("decal").forGetter(StencilData::decal),
            Codec.INT.optionalFieldOf("rgb", WHITE).forGetter(StencilData::rgb),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(StencilData::rotation),
            Codec.BOOL.optionalFieldOf("flip", false).forGetter(StencilData::flip),
            Codec.BOOL.optionalFieldOf("erase", false).forGetter(StencilData::erase)
    ).apply(i, StencilData::new));

    public static final StreamCodec<ByteBuf, StencilData> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), StencilData::decal,
            ByteBufCodecs.INT, StencilData::rgb,
            ByteBufCodecs.VAR_INT, StencilData::rotation,
            ByteBufCodecs.BOOL, StencilData::flip,
            ByteBufCodecs.BOOL, StencilData::erase,
            StencilData::new);

    public StencilData {
        rgb &= 0xFFFFFF;
        rotation = Math.floorMod(rotation, 4);
    }

    public static StencilData of(DecalState decal) {
        return new StencilData(Optional.of(decal.id()), decal.rgb(), decal.rotation(), decal.flip(), false);
    }

    public Optional<DecalState> toDecal() {
        return decal.map(id -> new DecalState(id, rgb, rotation, flip));
    }

    public StencilData withDecal(ResourceLocation id) {
        return new StencilData(Optional.of(id), rgb, rotation, flip, false);
    }

    public StencilData withRgb(int newRgb) {
        return new StencilData(decal, newRgb, rotation, flip, erase);
    }

    public StencilData withTurn(int newRotation, boolean newFlip) {
        return new StencilData(decal, rgb, newRotation, newFlip, erase);
    }

    public StencilData withErase(boolean newErase) {
        return new StencilData(decal, rgb, rotation, flip, newErase);
    }
}
