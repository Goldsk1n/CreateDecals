package com.goldskinmc.createdecals.stencil;

import com.goldskinmc.createdecals.decal.DecalPlacement;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.DecalText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * What a Decal Stencil holds, as a data component on the item: the chosen decal (none yet on a new stencil) or a typed
 * word ({@code text}, which wins when not empty), its colour, quarter turns and flip, whether it glows, which layer of a
 * face it goes on ({@code front}: over the decal already there), or the eraser. The decal's size in tiles comes from the
 * client's library when it is chosen (the server has none), and is clamped to {@link DecalPlacement#MAX_SIZE}.
 */
public record StencilData(Optional<ResourceLocation> decal, int rgb, int rotation, boolean flip, boolean erase, int width, int height,
        boolean front, boolean glow, String text) {
    public static final int WHITE = DyeColor.WHITE.getTextureDiffuseColor() & 0xFFFFFF;
    public static final StencilData EMPTY = new StencilData(Optional.empty(), WHITE, 0, false, false, 1, 1);

    public static final Codec<StencilData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("decal").forGetter(StencilData::decal),
            Codec.INT.optionalFieldOf("rgb", WHITE).forGetter(StencilData::rgb),
            Codec.INT.optionalFieldOf("rotation", 0).forGetter(StencilData::rotation),
            Codec.BOOL.optionalFieldOf("flip", false).forGetter(StencilData::flip),
            Codec.BOOL.optionalFieldOf("erase", false).forGetter(StencilData::erase),
            Codec.INT.optionalFieldOf("width", 1).forGetter(StencilData::width),
            Codec.INT.optionalFieldOf("height", 1).forGetter(StencilData::height),
            Codec.BOOL.optionalFieldOf("front", false).forGetter(StencilData::front),
            Codec.BOOL.optionalFieldOf("glow", false).forGetter(StencilData::glow),
            Codec.STRING.optionalFieldOf("text", "").forGetter(StencilData::text)
    ).apply(i, StencilData::new));

    private static final StreamCodec<ByteBuf, String> TEXT_CODEC = ByteBufCodecs.stringUtf8(DecalText.MAX_LENGTH * 4);

    private static final StreamCodec<ByteBuf, Optional<ResourceLocation>> DECAL_CODEC =
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional);

    /** The decal, the colour, then turns, flip, eraser, size, layer and glow packed in one number, then the word. */
    public static final StreamCodec<ByteBuf, StencilData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                DECAL_CODEC.encode(buf, data.decal());
                ByteBufCodecs.INT.encode(buf, data.rgb());
                ByteBufCodecs.VAR_INT.encode(buf, data.rotation() | (data.flip() ? 4 : 0) | (data.erase() ? 8 : 0)
                        | data.width() << 4 | data.height() << 8 | (data.front() ? 1 << 12 : 0) | (data.glow() ? 1 << 13 : 0));
                TEXT_CODEC.encode(buf, data.text());
            },
            buf -> {
                Optional<ResourceLocation> decal = DECAL_CODEC.decode(buf);
                int rgb = ByteBufCodecs.INT.decode(buf);
                int bits = ByteBufCodecs.VAR_INT.decode(buf);
                String text = TEXT_CODEC.decode(buf);
                return new StencilData(decal, rgb, bits & 3, (bits & 4) != 0, (bits & 8) != 0, (bits >> 4) & 15, (bits >> 8) & 15,
                        (bits & 1 << 12) != 0, (bits & 1 << 13) != 0, text);
            });

    public StencilData {
        rgb &= 0xFFFFFF;
        rotation = Math.floorMod(rotation, 4);
        width = Math.max(1, Math.min(DecalPlacement.MAX_SIZE, width));
        height = Math.max(1, Math.min(DecalPlacement.MAX_SIZE, height));
        text = DecalText.normalise(text == null ? "" : text);
    }

    /** A stencil for one decal, on the back layer, not glowing, no word. */
    public StencilData(Optional<ResourceLocation> decal, int rgb, int rotation, boolean flip, boolean erase, int width, int height) {
        this(decal, rgb, rotation, flip, erase, width, height, false, false, "");
    }

    /** A stencil holding {@code decal} (picked up from the {@code front} or back layer of a face). */
    public static StencilData of(DecalState decal, boolean front) {
        return new StencilData(Optional.of(decal.id()), decal.rgb(), decal.rotation(), decal.flip(), false, decal.width(), decal.height(),
                front, decal.glow(), "");
    }

    public static StencilData of(DecalState decal) {
        return of(decal, false);
    }

    /** Whether the stencil lays a typed word rather than its decal. */
    public boolean writes() {
        return !text.isBlank();
    }

    public Optional<DecalState> toDecal() {
        return decal.map(id -> new DecalState(id, rgb, rotation, flip, 0, 0, width, height, glow));
    }

    /** Choosing a decal ends the eraser and clears the word. */
    public StencilData withDecal(ResourceLocation id, int newWidth, int newHeight) {
        return new StencilData(Optional.of(id), rgb, rotation, flip, false, newWidth, newHeight, front, glow, "");
    }

    public StencilData withRgb(int newRgb) {
        return new StencilData(decal, newRgb, rotation, flip, erase, width, height, front, glow, text);
    }

    public StencilData withTurn(int newRotation, boolean newFlip) {
        return new StencilData(decal, rgb, newRotation, newFlip, erase, width, height, front, glow, text);
    }

    public StencilData withErase(boolean newErase) {
        return new StencilData(decal, rgb, rotation, flip, newErase, width, height, front, glow, text);
    }

    public StencilData withFront(boolean newFront) {
        return new StencilData(decal, rgb, rotation, flip, erase, width, height, newFront, glow, text);
    }

    public StencilData withGlow(boolean newGlow) {
        return new StencilData(decal, rgb, rotation, flip, erase, width, height, front, newGlow, text);
    }

    /** Typing a word ends the eraser; the chosen decal stays for when the word is cleared. */
    public StencilData withText(String newText) {
        return new StencilData(decal, rgb, rotation, flip, newText.isBlank() && erase, width, height, front, glow, newText);
    }
}
