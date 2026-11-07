package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.simibubi.create.content.decoration.copycat.CopycatStepModel;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * Create's copycat step model plus decals on any of its faces, each the part of the decal its face covers
 * ({@link DecalFaces}). Faces on the block's boundary come with that side, so they are culled with the step's own face
 * there; the faces inside the block (its back and its inner top or bottom) come with the unculled quads.
 */
public class DecalStepModel extends CopycatStepModel {
    public DecalStepModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
            ModelData blockEntityData) {
        blockEntityData = DecalPanelModel.withBlockEntityData(world, pos, blockEntityData);
        super.gatherModelData(builder, world, pos, state, blockEntityData);
        Map<Direction, DecalState> back = blockEntityData.get(DecalCopycatBlockEntity.DECALS);
        if (back != null) {
            builder.with(DecalCopycatBlockEntity.DECALS, back);
        }
        Map<Direction, DecalState> front = blockEntityData.get(DecalCopycatBlockEntity.FRONT_DECALS);
        if (front != null) {
            builder.with(DecalCopycatBlockEntity.FRONT_DECALS, front);
        }
        return builder;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return DecalQuads.LAYERS;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
            @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        if (state == null || renderType != DecalQuads.layer()
                || data.get(DecalCopycatBlockEntity.DECALS) == null && data.get(DecalCopycatBlockEntity.FRONT_DECALS) == null) {
            return quads;
        }
        long start = System.nanoTime();
        for (Direction face : Direction.values()) {
            DecalFaces.Window window = DecalFaces.window(state, face);
            if (window != null && (window.onBoundary() ? side == face : side == null)) {
                quads = DecalQuads.addLayers(quads, data, face, window);
            }
        }
        DecalQuads.decalNanos.addAndGet(System.nanoTime() - start);
        return quads;
    }
}
