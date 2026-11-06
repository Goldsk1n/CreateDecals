package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.decal.DecalFrame;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.panel.DecalPanelBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import com.simibubi.create.content.decoration.copycat.CopycatPanelModel;
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
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

/**
 * Create's copycat panel model plus one quad: the decal, laid on the panel's front face. It is an ordinary
 * baked quad on the block atlas, so it is meshed with the chunk (or contraption, or Sable vehicle) like any
 * block face and goes through every renderer and shader pack the way the material does.
 *
 * <p>The quad lies exactly in the plane of the material's front face, {@link #lift} blocks out, and is drawn
 * in a cutout layer, which every renderer draws after the solid one. With the same corners the two faces get the
 * same depth, and the depth test (less or equal) lets the later one win: no offset, so nothing to z-fight at a
 * distance. Its colour rides in the tint index ({@link #TINT_FLAG} | 0xRRGGBB), so tinting needs no block-entity
 * lookup wherever the panel is meshed.
 */
public class DecalPanelModel extends CopycatPanelModel {
    public static final int TINT_FLAG = 1 << 24;
    private static final ChunkRenderTypeSet LAYERS =
            ChunkRenderTypeSet.of(RenderType.solid(), RenderType.cutout(), RenderType.cutoutMipped(), RenderType.translucent());
    private static final float PANEL_DEPTH = 3 / 16f;

    /** How far the decal stands off the face, in blocks (a tuning knob for checks; 0 = coplanar). */
    public static volatile float lift = Float.parseFloat(System.getProperty("createdecals.decalLift", "0"));
    /** The layer decal quads go into: {@code cutout} or {@code cutout_mipped} (a tuning knob for checks). */
    public static volatile boolean mipped = Boolean.parseBoolean(System.getProperty("createdecals.decalMipped", "true"));

    /** Time spent baking chunks in this model, all of it and the decal part alone, for the frame-cost check. */
    public static final AtomicLong totalNanos = new AtomicLong();
    public static final AtomicLong decalNanos = new AtomicLong();
    public static final AtomicLong calls = new AtomicLong();

    private static final Map<Long, BakedQuad> QUADS = new ConcurrentHashMap<>();

    public DecalPanelModel(BakedModel originalModel) {
        super(originalModel);
    }

    /** Forgets baked decal quads, after the atlas or a tuning knob changed. */
    public static void clearCache() {
        QUADS.clear();
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
            ModelData blockEntityData) {
        super.gatherModelData(builder, world, pos, state, blockEntityData);
        DecalState decal = blockEntityData.get(DecalPanelBlockEntity.DECAL);
        if (decal != null) {
            builder.with(DecalPanelBlockEntity.DECAL, decal);
        }
        return builder;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return LAYERS;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
            @Nullable RenderType renderType) {
        long start = System.nanoTime();
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        // The decal sits 3/16 into the block, on no block boundary, so it comes with the unculled (null side) quads
        DecalState decal = data.get(DecalPanelBlockEntity.DECAL);
        if (decal != null && side == null && state != null && renderType == decalLayer()) {
            long decalStart = System.nanoTime();
            Direction facing = state.getOptionalValue(CopycatPanelBlock.FACING).orElse(Direction.UP);
            BakedQuad quad = QUADS.computeIfAbsent(decal.key() ^ ((long) facing.ordinal() << 60), k -> bake(decal, facing));
            List<BakedQuad> withDecal = new ArrayList<>(quads.size() + 1);
            withDecal.addAll(quads);
            withDecal.add(quad);
            quads = withDecal;
            decalNanos.addAndGet(System.nanoTime() - decalStart);
        }
        totalNanos.addAndGet(System.nanoTime() - start);
        calls.incrementAndGet();
        return quads;
    }

    private static RenderType decalLayer() {
        return mipped ? RenderType.cutoutMipped() : RenderType.cutout();
    }

    /**
     * The decal on the face pointing {@code facing}: corners counter-clockwise seen from the front (top left,
     * bottom left, bottom right, top right), as vanilla bakes faces, so it gets the same shading and AO.
     */
    static BakedQuad bake(DecalState decal, Direction facing) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(decal.decal().texture());
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 right = Vec3.atLowerCornerOf(DecalFrame.right(facing).getNormal());
        Vec3 up = normal.cross(right);
        Vec3 centre = new Vec3(0.5, 0.5, 0.5).add(normal.scale(-0.5 + PANEL_DEPTH + lift));
        Vec3[] corners = {
                centre.add(right.scale(-0.5)).add(up.scale(0.5)),
                centre.add(right.scale(-0.5)).add(up.scale(-0.5)),
                centre.add(right.scale(0.5)).add(up.scale(-0.5)),
                centre.add(right.scale(0.5)).add(up.scale(0.5)),
        };
        float[][] uvs = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};

        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        builder.setSprite(sprite);
        builder.setDirection(facing);
        builder.setTintIndex(TINT_FLAG | decal.rgb());
        builder.setShade(true);
        builder.setHasAmbientOcclusion(true);
        for (int i = 0; i < 4; i++) {
            // A quarter turn clockwise hands each corner the texture corner one step on, counter-clockwise
            float[] uv = uvs[(i + decal.rotation()) % 4];
            float u = decal.flip() ? 1 - uv[0] : uv[0];
            Vec3 c = corners[i];
            builder.addVertex((float) c.x, (float) c.y, (float) c.z)
                    .setColor(-1)
                    .setUv(sprite.getU(u), sprite.getV(uv[1]))
                    .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
        }
        return builder.bakeQuad();
    }
}
