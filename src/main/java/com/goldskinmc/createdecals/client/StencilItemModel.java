package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

/**
 * The Decal Stencil's icon with its chosen decal on the plate: the whole picture (every tile of a large one), tinted,
 * flipped and turned as it will lie on a face, over the plate's middle. Without a decal (or as the eraser) it is the
 * plain icon. One model per choice, cached.
 */
public class StencilItemModel extends BakedModelWrapper<BakedModel> {
    private static final Map<StencilData, BakedModel> CACHE = new ConcurrentHashMap<>();
    /** Every colour and turn chosen is an entry: past this many the cache is emptied and refilled by the stencils in use. */
    private static final int CACHE_LIMIT = 512;

    private final ItemOverrides overrides = new ItemOverrides() {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            StencilData data = DecalStencilItem.data(stack);
            if (data.erase() || data.decal().isEmpty()) {
                return StencilItemModel.this.originalModel;
            }
            DecalLibrary.Entry entry = DecalLibrary.INSTANCE.get(data.decal().get());
            if (entry == null) {
                return StencilItemModel.this.originalModel;
            }
            if (CACHE.size() >= CACHE_LIMIT && !CACHE.containsKey(data)) {
                CACHE.clear();
            }
            return CACHE.computeIfAbsent(data, d -> new WithDecal(StencilItemModel.this.originalModel, quad(entry, d)));
        }
    };

    public StencilItemModel(BakedModel originalModel) {
        super(originalModel);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    /** The decal over the plate's middle 10 x 10 pixels, just in front of the item's front (south) face. */
    private static BakedQuad quad(DecalLibrary.Entry entry, StencilData data) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(entry.sprite());
        float lo = 3 / 16f;
        float hi = 13 / 16f;
        float z = 8.5f / 16 + 0.002f;
        float[][] corners = {{lo, hi}, {lo, lo}, {hi, lo}, {hi, hi}};
        float[][] uvs = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        builder.setSprite(sprite);
        builder.setDirection(Direction.SOUTH);
        builder.setTintIndex(DecalQuads.TINT_FLAG | data.rgb());
        builder.setShade(false);
        for (int i = 0; i < 4; i++) {
            float[] uv = uvs[(i + data.rotation()) % 4];
            float u = data.flip() ? 1 - uv[0] : uv[0];
            builder.addVertex(corners[i][0], corners[i][1], z)
                    .setColor(-1)
                    .setUv(sprite.getU(u), sprite.getV(uv[1]))
                    .setNormal(0, 0, 1);
        }
        return builder.bakeQuad();
    }

    /** The plate with the decal on it instead of the chevron. */
    private static final class WithDecal extends BakedModelWrapper<BakedModel> {
        private final BakedQuad decal;

        WithDecal(BakedModel original, BakedQuad decal) {
            super(original);
            this.decal = decal;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return withDecal(super.getQuads(state, side, rand), side);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data,
                @Nullable RenderType renderType) {
            return withDecal(super.getQuads(state, side, rand, data, renderType), side);
        }

        private List<BakedQuad> withDecal(List<BakedQuad> quads, @Nullable Direction side) {
            if (side != null) {
                return quads;
            }
            // The plate stays; the chevron layer (tint index 1) gives way to the decal
            List<BakedQuad> all = new ArrayList<>(quads.size() + 1);
            for (BakedQuad quad : quads) {
                if (quad.getTintIndex() != 1) {
                    all.add(quad);
                }
            }
            all.add(decal);
            return all;
        }

        /** Item rendering asks the model for its passes; the wrapped model would answer with itself, losing the decal. */
        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }

        /** The same for the display transform (gui, hand, ground): apply it, but stay the model that is drawn. */
        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
            super.applyTransform(context, poseStack, leftHand);
            return this;
        }
    }
}
