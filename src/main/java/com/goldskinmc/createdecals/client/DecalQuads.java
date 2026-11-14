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
 * cached by decal, face and depth. On other mods' copycats (Copycats+) a decal is {@link #projected} onto each of their
 * quads that {@link #faces} its face.
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

    /** How far a quad must face a face to take its decal: square on, or slanted up to about 70 degrees from it. */
    private static final float FACING = 0.35f;

    private record Key(DecalState decal, Direction face, DecalFaces.Window window, float lift) {
    }

    private static final Map<Key, BakedQuad> CACHE = new ConcurrentHashMap<>();
    /**
     * The cache's bound: every colour, turn and window of a decal is an entry, so hex colours and long sessions would
     * grow it without end. Past this it is emptied and refilled by what is in view (a quad is a few hundred bytes).
     */
    static final int CACHE_LIMIT = 50_000;

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
        return addLayers(quads, data, face, new DecalFaces.Window(depth, 0, 1, 0, 1));
    }

    /** {@link #addLayers(List, ModelData, Direction, float)} for a decal seen through a window of the face (steps). */
    public static List<BakedQuad> addLayers(List<BakedQuad> quads, ModelData data, Direction face, DecalFaces.Window window) {
        BakedQuad back = quadIn(data.get(DecalCopycatBlockEntity.DECALS), face, window);
        BakedQuad front = quadIn(data.get(DecalCopycatBlockEntity.FRONT_DECALS), face, window);
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
    private static BakedQuad quadIn(@Nullable Map<Direction, DecalState> layer, Direction face, DecalFaces.Window window) {
        DecalState decal = layer != null ? layer.get(face) : null;
        return decal != null ? quad(decal, face, window) : null;
    }

    /**
     * The decal on the face pointing {@code face}, {@code depth} blocks from the block's far side along it (1 for a full
     * block's face, 3/16 for a panel's front), or null if the decal is not in the library.
     */
    @Nullable
    public static BakedQuad quad(DecalState decal, Direction face, float depth) {
        return quad(decal, face, new DecalFaces.Window(depth, 0, 1, 0, 1));
    }

    /** The part of the decal a window of the face shows, or null if the decal is not in the library. */
    @Nullable
    public static BakedQuad quad(DecalState decal, Direction face, DecalFaces.Window window) {
        Key key = new Key(decal, face, window, lift);
        BakedQuad cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        DecalLibrary.Entry entry = DecalLibrary.INSTANCE.get(decal.id());
        if (entry == null) {
            return null;
        }
        BakedQuad quad = bake(entry, decal, face, window, key.lift());
        if (CACHE.size() >= CACHE_LIMIT) {
            CACHE.clear();
        }
        CACHE.put(key, quad);
        return quad;
    }

    private static TextureAtlasSprite sprite(DecalLibrary.Entry entry) {
        return Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(entry.sprite());
    }

    /** Corners counter-clockwise seen from the front (top left, bottom left, bottom right, top right), as vanilla bakes faces. */
    private static BakedQuad bake(DecalLibrary.Entry entry, DecalState decal, Direction face, DecalFaces.Window window, float lift) {
        TextureAtlasSprite sprite = sprite(entry);
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 right = Vec3.atLowerCornerOf(DecalFrame.right(face).getNormal());
        Vec3 up = Vec3.atLowerCornerOf(DecalFrame.up(face).getNormal());
        Vec3 centre = new Vec3(0.5, 0.5, 0.5).add(normal.scale(-0.5 + window.depth() + lift));
        // The window's corners as (from the left, from the top) of the face: top left, bottom left, bottom right, top right
        float[][] st = {
                {window.s0(), window.t0()}, {window.s0(), window.t1()}, {window.s1(), window.t1()}, {window.s1(), window.t0()},
        };
        QuadBakingVertexConsumer builder = start(sprite, decal, face);
        for (int i = 0; i < 4; i++) {
            Vec3 c = centre.add(right.scale(st[i][0] - 0.5)).add(up.scale(0.5 - st[i][1]));
            float[] uv = uv(decal, sprite, st[i][0], st[i][1]);
            builder.addVertex((float) c.x, (float) c.y, (float) c.z)
                    .setColor(-1)
                    .setUv(uv[0], uv[1])
                    .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
        }
        return finish(builder, decal);
    }

    private static QuadBakingVertexConsumer start(TextureAtlasSprite sprite, DecalState decal, Direction face) {
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        builder.setSprite(sprite);
        builder.setDirection(face);
        builder.setTintIndex(TINT_FLAG | decal.rgb());
        // A glowing decal is not shaded by the face's direction or darkened in corners, and its baked light is full
        builder.setShade(!decal.glow());
        builder.setHasAmbientOcclusion(!decal.glow());
        return builder;
    }

    private static BakedQuad finish(QuadBakingVertexConsumer builder, DecalState decal) {
        BakedQuad quad = builder.bakeQuad();
        return decal.glow() ? QuadTransformers.settingMaxEmissivity().process(quad) : quad;
    }

    /**
     * The sprite coordinates at ({@code s}, {@code t}) of the face (from its left and top, 0-1). The whole face's corners
     * take the texture's corners turned and flipped (a quarter turn clockwise hands each corner the texture corner one
     * step on, counter-clockwise), and a point between them the same mix of them: top left + s (to top right) + t (to
     * bottom left). Then the tile's part of the sprite, for a decal that spans several blocks.
     */
    private static float[] uv(DecalState decal, TextureAtlasSprite sprite, float s, float t) {
        float[][] uvs = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        float[][] corner = new float[4][];
        for (int i = 0; i < 4; i++) {
            float[] uv = uvs[(i + decal.rotation()) % 4];
            corner[i] = new float[] {decal.flip() ? 1 - uv[0] : uv[0], uv[1]};
        }
        float u = corner[0][0] + s * (corner[3][0] - corner[0][0]) + t * (corner[1][0] - corner[0][0]);
        float v = corner[0][1] + s * (corner[3][1] - corner[0][1]) + t * (corner[1][1] - corner[0][1]);
        return new float[] {sprite.getU((decal.tileX() + u) / decal.width()), sprite.getV((decal.tileY() + v) / decal.height())};
    }

    private static float[] position(int[] vertices, int vertex) {
        int at = vertex * (vertices.length / 4);
        return new float[] {Float.intBitsToFloat(vertices[at]), Float.intBitsToFloat(vertices[at + 1]), Float.intBitsToFloat(vertices[at + 2])};
    }

    /**
     * Whether a quad of another mod's copycat (Copycats+) faces {@code face}, so a decal on that face goes on it: its
     * normal, from its corners, points that way (a slope's slanted top faces both up and its low side).
     */
    public static boolean faces(BakedQuad quad, Direction face) {
        int[] v = quad.getVertices();
        float[] p0 = position(v, 0);
        float[] p1 = position(v, 1);
        float[] p2 = position(v, 2);
        float ax = p1[0] - p0[0], ay = p1[1] - p0[1], az = p1[2] - p0[2];
        float bx = p2[0] - p0[0], by = p2[1] - p0[1], bz = p2[2] - p0[2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1e-6f) {
            return quad.getDirection() == face;
        }
        return (nx * face.getStepX() + ny * face.getStepY() + nz * face.getStepZ()) / length > FACING;
    }

    /**
     * The decal on {@code face} projected onto one quad of another mod's copycat (Copycats+): the same corners as that
     * quad (so it wins the depth tie over it, as our decals do over their material), each taking the part of the decal
     * that lies there seen square on to {@code face}, as if the decal lay on the block's face in that plane. Null if the
     * decal is not in the library.
     */
    @Nullable
    public static BakedQuad projected(DecalState decal, Direction face, BakedQuad material) {
        DecalLibrary.Entry entry = DecalLibrary.INSTANCE.get(decal.id());
        if (entry == null) {
            return null;
        }
        TextureAtlasSprite sprite = sprite(entry);
        Direction right = DecalFrame.right(face);
        Direction up = DecalFrame.up(face);
        Direction normal = material.getDirection();
        int[] v = material.getVertices();
        QuadBakingVertexConsumer builder = start(sprite, decal, normal);
        for (int i = 0; i < 4; i++) {
            float[] p = position(v, i);
            float alongRight = p[right.getAxis().ordinal()];
            float alongUp = p[up.getAxis().ordinal()];
            float s = right.getAxisDirection() == Direction.AxisDirection.POSITIVE ? alongRight : 1 - alongRight;
            float t = up.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 - alongUp : alongUp;
            float[] uv = uv(decal, sprite, s, t);
            builder.addVertex(p[0], p[1], p[2])
                    .setColor(-1)
                    .setUv(uv[0], uv[1])
                    .setNormal(normal.getStepX(), normal.getStepY(), normal.getStepZ());
        }
        return finish(builder, decal);
    }
}
