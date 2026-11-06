package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A full-cube copycat: the material's own quads, and a decal on each face that has one. A face's quads come with that
 * face's side, so the decal is culled with the face; faces Create's copycat logic hides (against another copycat,
 * glass against glass) hide their decals too, from the mask gathered here the same way.
 */
public class DecalBlockModel extends CopycatModel {
    private static final ModelProperty<Integer> HIDDEN_FACES = new ModelProperty<>();

    public DecalBlockModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
            ModelData blockEntityData) {
        super.gatherModelData(builder, world, pos, state, blockEntityData);
        Map<Direction, DecalState> decals = blockEntityData.get(DecalCopycatBlockEntity.DECALS);
        if (decals == null) {
            return builder;
        }
        builder.with(DecalCopycatBlockEntity.DECALS, decals);
        BlockState material = getMaterial(blockEntityData);
        BlockPos.MutableBlockPos neighbourPos = new BlockPos.MutableBlockPos();
        int hidden = 0;
        for (Direction face : decals.keySet()) {
            neighbourPos.setWithOffset(pos, face);
            BlockState neighbour = world.getBlockState(neighbourPos);
            if (neighbour.hidesNeighborFace(world, neighbourPos, state, face.getOpposite())
                    || !Block.shouldRenderFace(material, world, pos, face, neighbourPos)) {
                hidden |= 1 << face.ordinal();
            }
        }
        return builder.with(HIDDEN_FACES, hidden);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return DecalQuads.LAYERS;
    }

    /** The material's quads, copied: Create may make them emissive in place. */
    @Override
    protected List<BakedQuad> getCroppedQuads(BlockState state, Direction side, RandomSource rand, BlockState material,
            ModelData wrappedData, RenderType renderType) {
        List<BakedQuad> template = getModelOf(material).getQuads(material, side, rand, wrappedData, renderType);
        List<BakedQuad> quads = new ArrayList<>(template.size());
        for (BakedQuad quad : template) {
            quads.add(BakedQuadHelper.clone(quad));
        }
        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
            @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        Map<Direction, DecalState> decals = data.get(DecalCopycatBlockEntity.DECALS);
        if (decals == null || side == null || renderType != DecalQuads.layer()) {
            return quads;
        }
        DecalState decal = decals.get(side);
        Integer hidden = data.get(HIDDEN_FACES);
        if (decal == null || hidden != null && (hidden & 1 << side.ordinal()) != 0) {
            return quads;
        }
        long start = System.nanoTime();
        BakedQuad quad = DecalQuads.quad(decal, side, 1);
        if (quad != null) {
            List<BakedQuad> withDecal = new ArrayList<>(quads.size() + 1);
            withDecal.addAll(quads);
            withDecal.add(quad);
            quads = withDecal;
        }
        DecalQuads.decalNanos.addAndGet(System.nanoTime() - start);
        return quads;
    }
}
