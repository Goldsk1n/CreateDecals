package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.block.AttachedDecals;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.registry.DecalsAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Round the model of another mod's copycat (Copycats+): its quads as they are, plus, where its block entity carries
 * decals ({@link AttachedDecals}), each decal projected onto every quad that faces its face ({@link DecalQuads#faces}),
 * in our decal layer, culled with the quad it lies on. The copycat's shape is read from its own quads, so slopes,
 * stairs, beams and doors need nothing of their own; the decals reach the model through its model data, which chunk
 * building and Create's contraption renderer both ask for with the block entity at hand.
 */
public class AttachedDecalModel extends BakedModelWrapper<BakedModel> {
    public static final ModelProperty<AttachedDecals> DECALS = new ModelProperty<>();

    public AttachedDecalModel(BakedModel copycat) {
        super(copycat);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        ModelData inner = super.getModelData(level, pos, state, data);
        BlockEntity be = level.getBlockEntity(pos);
        AttachedDecals decals = be != null ? be.getExistingData(DecalsAttachments.DECALS.get()).orElse(null) : null;
        return decals == null || decals.isEmpty() ? inner : inner.derive().with(DECALS, decals).build();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        ChunkRenderTypeSet inner = super.getRenderTypes(state, rand, data);
        return data.get(DECALS) == null ? inner : ChunkRenderTypeSet.union(inner, ChunkRenderTypeSet.of(DecalQuads.layer()));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
            @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        AttachedDecals decals = data.get(DECALS);
        if (decals == null || state == null || renderType != null && renderType != DecalQuads.layer()) {
            return quads;
        }
        long start = System.nanoTime();
        List<BakedQuad> out = new ArrayList<>(quads);
        for (BakedQuad material : materialQuads(state, side, rand, data)) {
            addProjected(out, decals, material);
        }
        DecalQuads.decalNanos.addAndGet(System.nanoTime() - start);
        return out;
    }

    /** The copycat's own quads in {@code side}'s bucket, from every layer its material draws in. */
    private List<BakedQuad> materialQuads(BlockState state, @Nullable Direction side, RandomSource rand, ModelData data) {
        List<BakedQuad> material = new ArrayList<>();
        for (RenderType type : super.getRenderTypes(state, rand, data)) {
            material.addAll(super.getQuads(state, side, rand, data, type));
        }
        return material;
    }

    /** The back then the front decal of each face {@code material} faces, projected onto it. */
    private static void addProjected(List<BakedQuad> out, AttachedDecals decals, BakedQuad material) {
        for (boolean front : new boolean[] {false, true}) {
            for (Direction face : Direction.values()) {
                DecalState decal = decals.get(face, front);
                if (decal != null && DecalQuads.faces(material, face)) {
                    BakedQuad quad = DecalQuads.projected(decal, face, material);
                    if (quad != null) {
                        out.add(quad);
                    }
                }
            }
        }
    }

    /** A decal quad for the contraption glow pass, and whether it is drawn at full light. */
    public record GlowQuad(BakedQuad quad, boolean glows) {
    }

    /**
     * For {@link ContraptionGlow}: the glowing decals of a copycat on a contraption, projected as the model draws them,
     * and a front decal over a glowing back one (at the world's light, so the layers stay in order); faces the model
     * culls against a solid neighbour are left out. Empty when the model there is not ours.
     */
    public static List<GlowQuad> glowQuads(BakedModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, AttachedDecals decals) {
        if (!(model instanceof AttachedDecalModel wrapper)) {
            return List.of();
        }
        ModelData data = wrapper.getModelData(level, pos, state, level.getModelData(pos));
        RandomSource rand = RandomSource.create(state.getSeed(pos));
        List<GlowQuad> out = new ArrayList<>();
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        for (Direction side : sides) {
            if (side != null && level.getBlockState(pos.relative(side)).isSolidRender(level, pos.relative(side))) {
                continue;
            }
            for (BakedQuad material : wrapper.materialQuads(state, side, rand, data)) {
                for (Direction face : Direction.values()) {
                    DecalState back = decals.get(face, false);
                    DecalState front = decals.get(face, true);
                    boolean backGlows = back != null && back.glow();
                    if (!backGlows && (front == null || !front.glow()) || !DecalQuads.faces(material, face)) {
                        continue;
                    }
                    if (backGlows) {
                        BakedQuad quad = DecalQuads.projected(back, face, material);
                        if (quad != null) {
                            out.add(new GlowQuad(quad, true));
                        }
                    }
                    if (front != null) {
                        BakedQuad quad = DecalQuads.projected(front, face, material);
                        if (quad != null) {
                            out.add(new GlowQuad(quad, front.glow()));
                        }
                    }
                }
            }
        }
        return out;
    }

    /**
     * For the stencil's ghost: {@code decal} on {@code face} of the copycat at {@code pos}, projected onto its quads as
     * the model would draw it (all of them, culled or not). Empty when the model there is not ours.
     */
    public static List<BakedQuad> ghostQuads(BakedModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, DecalState decal,
            Direction face) {
        if (!(model instanceof AttachedDecalModel wrapper)) {
            return List.of();
        }
        ModelData data = wrapper.getModelData(level, pos, state, level.getModelData(pos));
        RandomSource rand = RandomSource.create(state.getSeed(pos));
        List<BakedQuad> out = new ArrayList<>();
        AttachedDecals only = AttachedDecals.EMPTY.with(face, false, decal);
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        for (Direction side : sides) {
            for (BakedQuad material : wrapper.materialQuads(state, side, rand, data)) {
                addProjected(out, only, material);
            }
        }
        return out;
    }
}
