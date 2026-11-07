package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalFrame;
import com.goldskinmc.createdecals.decal.DecalState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

/**
 * Bakes a decal as one quad lying exactly in the plane of a face of the copied material, in the cutout layer. Every
 * renderer draws cutout after solid with a less-or-equal depth test, so with the same corners the decal wins the tie:
 * no offset, nothing to z-fight at a distance. Its colour rides in the tint index ({@link #TINT_FLAG} | 0xRRGGBB), so
 * tinting needs no block-entity lookup wherever the block is meshed (chunks, contraptions, Sable plots). Quads are
 * cached by decal, face and depth.
 */
public final class DecalQuads {
    public static final int TINT_FLAG = 1 << 24;
    /** The layers a copycat may draw in: its material decides, the decal is always in {@link #layer()}. */
    public static final ChunkRenderTypeSet LAYERS =
            ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout(), RenderType.cutoutMipped(), RenderType.translucent());

    /** How far the decal stands off the face, in blocks (a tuning knob for checks; 0 = coplanar). */
    public static volatile float lift = Float.parseFloat(System.getProperty("createdecals.decalLift", "0"));
    /** Decals in {@code cutout_mipped} (true) or {@code cutout} (a tuning knob for checks). */
    public static volatile boolean mipped = Boolean.parseBoolean(System.getProperty("createdecals.decalMipped", "true"));

    /** Time spent adding decals while meshing, for the frame-cost checks. */
    public static final AtomicLong decalNanos = new AtomicLong();

    private record Key(DecalState decal, Direction face, float depth, float lift) {
    }

    private static final Map<Key, BakedQuad> CACHE = new ConcurrentHashMap<>();

    private DecalQuads() {
    }

    public static RenderType layer() {
        return mipped ? RenderType.cutoutMipped() : RenderType.cutout();
    }

    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * {@code quads} plus the face's back decal and then its front one, both from the model data. Drawn later in the same
     * layer and with the same corners, the front decal wins the depth tie over the back one, as the back one does over
     * the material.
     */
    public static List<BakedQuad> addLayers(List<BakedQuad> quads, ModelData data, Direction face, float depth) {
        BakedQuad back = quadIn(data.get(DecalCopycatBlockEntity.DECALS), face, depth);
        BakedQuad front = quadIn(data.get(DecalCopycatBlockEntity.FRONT_DECALS), face, depth);
        if (back == null && front == null) {
            return quads;
        }
        List<BakedQuad> out = new ArrayList<>(quads.size() + 2);
        out.addAll(quads);
        if (back != null) {
            out.add(back);
        }
        if (front != null) {
            out.add(front);
        }
        return out;
    }

    @Nullable
    private static BakedQuad quadIn(@Nullable Map<Direction, DecalState> layer, Direction face, float depth) {
        DecalState decal = layer != null ? layer.get(face) : null;
        return decal != null ? quad(decal, face, depth) : null;
    }

    /**
     * The decal on the face pointing {@code face}, {@code depth} blocks from the block's far side along it (1 for a full
     * block's face, 3/16 for a panel's front), or null if the decal is not in the library.
     */
    @Nullable
    public static BakedQuad quad(DecalState decal, Direction face, float depth) {
        Key key = new Key(decal, face, depth, lift);
        BakedQuad cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        DecalLibrary.Entry entry = DecalLibrary.INSTANCE.get(decal.id());
        if (entry == null) {
            return null;
        }
        BakedQuad quad = bake(entry, decal, face, depth, key.lift());
        CACHE.put(key, quad);
        return quad;
    }

    /** Corners counter-clockwise seen from the front (top left, bottom left, bottom right, top right), as vanilla bakes faces. */
    private static BakedQuad bake(DecalLibrary.Entry entry, DecalState decal, Direction face, float depth, float lift) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(entry.sprite());
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 right = Vec3.atLowerCornerOf(DecalFrame.right(face).getNormal());
        Vec3 up = Vec3.atLowerCornerOf(DecalFrame.up(face).getNormal());
        Vec3 centre = new Vec3(0.5, 0.5, 0.5).add(normal.scale(-0.5 + depth + lift));
        Vec3[] corners = {
                centre.add(right.scale(-0.5)).add(up.scale(0.5)),
                centre.add(right.scale(-0.5)).add(up.scale(-0.5)),
                centre.add(right.scale(0.5)).add(up.scale(-0.5)),
                centre.add(right.scale(0.5)).add(up.scale(0.5)),
        };
        float[][] uvs = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        // This face shows one tile of a decal that may span several blocks
        int tileX = decal.tileX();
        int tileY = decal.tileY();

        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        builder.setSprite(sprite);
        builder.setDirection(face);
        builder.setTintIndex(TINT_FLAG | decal.rgb());
        // A glowing decal is not shaded by the face's direction or darkened in corners, and its baked light is full
        builder.setShade(!decal.glow());
        builder.setHasAmbientOcclusion(!decal.glow());
        for (int i = 0; i < 4; i++) {
            // A quarter turn clockwise hands each corner the texture corner one step on, counter-clockwise
            float[] uv = uvs[(i + decal.rotation()) % 4];
            float u = decal.flip() ? 1 - uv[0] : uv[0];
            Vec3 c = corners[i];
            builder.addVertex((float) c.x, (float) c.y, (float) c.z)
                    .setColor(-1)
                    .setUv(sprite.getU((tileX + u) / decal.width()), sprite.getV((tileY + uv[1]) / decal.height()))
                    .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
        }
        BakedQuad quad = builder.bakeQuad();
        return decal.glow() ? QuadTransformers.settingMaxEmissivity().process(quad) : quad;
    }
}
