package com.goldskinmc.createdecals.stencil;

import com.goldskinmc.createdecals.decal.DecalPlacement;
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
 * quarter turns and flip, or the eraser. The decal's size in tiles comes from the client's library when it is chosen
 * (the server has none), and is clamped to {@link DecalPlacement#MAX_SIZE}.
 */
public record StencilData(Optional<ResourceLocation> decal, int rgb, int rotation, boolean flip, boolean erase, int width, int height) {
    public static final int WHITE = DyeColor.WHITE.getTextureDiffuseColor() & 0xFFFFFF;
    public static final StencilData EMPTY = new StencilData(Optional.empty(), WHITE, 0, false, false, 1, 1);

    public static final Codec<StencilData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("decal").forGetter(StencilData::decal),
            Codec.INT.optionalFieldOf("rgb", WHITE).forGetter(StencilData::rgb),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(StencilData::rotation),
            Codec.BOOL.optionalFieldOf("flip", false).forGetter(StencilData::flip),
            Codec.BOOL.optionalFieldOf("erase", false).forGetter(StencilData::erase),
            Codec.INT.optionalFieldOf("width", 1).forGetter(StencilData::width),
            Codec.INT.optionalFieldOf("height", 1).forGetter(StencilData::height)
    ).apply(i, StencilData::new));

    private static final StreamCodec<ByteBuf, Optional<ResourceLocation>> DECAL_CODEC =
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional);

    /** The decal, the colour, then turns, flip, eraser and size packed in one number. */
    public static final StreamCodec<ByteBuf, StencilData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                DECAL_CODEC.encode(buf, data.decal());
                ByteBufCodecs.INT.encode(buf, data.rgb());
                ByteBufCodecs.VAR_INT.encode(buf, data.rotation() | (data.flip() ? 4 : 0) | (data.erase() ? 8 : 0)
                        | data.width() << 4 | data.height() << 8);
            },
            buf -> {
                Optional<ResourceLocation> decal = DECAL_CODEC.decode(buf);
                int rgb = ByteBufCodecs.INT.decode(buf);
                int bits = ByteBufCodecs.VAR_INT.decode(buf);
                return new StencilData(decal, rgb, bits & 3, (bits & 4) != 0, (bits & 8) != 0, (bits >> 4) & 15, (bits >> 8) & 15);
            });

    public StencilData {
        rgb &= 0xFFFFFF;
        rotation = Math.floorMod(rotation, 4);
        width = Math.max(1, Math.min(DecalPlacement.MAX_SIZE, width));
        height = Math.max(1, Math.min(DecalPlacement.MAX_SIZE, height));
    }

    public static StencilData of(DecalState decal) {
        return new StencilData(Optional.of(decal.id()), decal.rgb(), decal.rotation(), decal.flip(), false, decal.width(), decal.height());
    }

    public Optional<DecalState> toDecal() {
        return decal.map(id -> new DecalState(id, rgb, rotation, flip, 0, 0, width, height));
    }

    public StencilData withDecal(ResourceLocation id, int newWidth, int newHeight) {
        return new StencilData(Optional.of(id), rgb, rotation, flip, false, newWidth, newHeight);
    }

    public StencilData withRgb(int newRgb) {
        return new StencilData(decal, newRgb, rotation, flip, erase, width, height);
    }

    public StencilData withTurn(int newRotation, boolean newFlip) {
        return new StencilData(decal, rgb, newRotation, newFlip, erase, width, height);
    }

    public StencilData withErase(boolean newErase) {
        return new StencilData(decal, rgb, rotation, flip, newErase, width, height);
    }
}
