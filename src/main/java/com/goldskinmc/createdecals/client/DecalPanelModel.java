package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import com.simibubi.create.content.decoration.copycat.CopycatPanelModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
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
 * Create's copycat panel model plus the decal on the front face. That face sits 3/16 into the block, on no block
 * boundary, so its quads come with the unculled ({@code null} side) ones, and so does the decal.
 */
public class DecalPanelModel extends CopycatPanelModel {
    private static final float FRONT = 3 / 16f;

    /** Time spent in this model while meshing, for the frame-cost checks. */
    public static final AtomicLong totalNanos = new AtomicLong();
    public static final AtomicLong calls = new AtomicLong();

    public DecalPanelModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
            ModelData blockEntityData) {
        blockEntityData = withBlockEntityData(world, pos, blockEntityData);
        super.gatherModelData(builder, world, pos, state, blockEntityData);
        Map<Direction, DecalState> decals = blockEntityData.get(DecalCopycatBlockEntity.DECALS);
        if (decals != null) {
            builder.with(DecalCopycatBlockEntity.DECALS, decals);
        }
        return builder;
    }

    /**
     * The block entity's own data when the renderer passed none: Ponder's world gives models no block-entity data, so a
     * copycat there would show neither its material nor its decals. World meshing always passes it, so never gets here.
     */
    static ModelData withBlockEntityData(BlockAndTintGetter world, BlockPos pos, ModelData blockEntityData) {
        if (blockEntityData.get(CopycatModel.MATERIAL_PROPERTY) == null && world.getBlockEntity(pos) instanceof DecalCopycatBlockEntity be) {
            return be.getModelData();
        }
        return blockEntityData;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return DecalQuads.LAYERS;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
            @Nullable RenderType renderType) {
        long start = System.nanoTime();
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        Map<Direction, DecalState> decals = data.get(DecalCopycatBlockEntity.DECALS);
        if (decals != null && side == null && state != null && renderType == DecalQuads.layer()) {
            long decalStart = System.nanoTime();
            Direction front = state.getOptionalValue(CopycatPanelBlock.FACING).orElse(Direction.UP);
            DecalState decal = decals.get(front);
            BakedQuad quad = decal != null ? DecalQuads.quad(decal, front, FRONT) : null;
            if (quad != null) {
                List<BakedQuad> withDecal = new ArrayList<>(quads.size() + 1);
                withDecal.addAll(quads);
                withDecal.add(quad);
                quads = withDecal;
            }
            DecalQuads.decalNanos.addAndGet(System.nanoTime() - decalStart);
        }
        totalNanos.addAndGet(System.nanoTime() - start);
        calls.incrementAndGet();
        return quads;
    }
}
