package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import com.goldskinmc.createdecals.block.AttachedDecals;
import com.goldskinmc.createdecals.registry.DecalsAttachments;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Glowing decals on a moving Create contraption, drawn a second time at full light over the contraption's own mesh.
 * The mesh has them already, and they glow there in vanilla and under shader packs; under Sodium they come out at the
 * light round the block (see {@code contraption/DecalMovement}). This copy is lit by the light handed to it, which every
 * renderer keeps, and drawn with a polygon offset (Minecraft's sign-text layer) so it wins over the mesh's copy on the
 * same face without flicker. A front decal over a glowing back one is drawn too, at the world's light, so the layers stay
 * in order. Faces against a solid block are skipped, as the model hides them.
 */
public final class ContraptionGlow {
    private ContraptionGlow() {
    }

    public static void render(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices,
            MultiBufferSource buffer) {
        BlockEntity blockEntity = renderWorld.getBlockEntity(context.localPos);
        if (blockEntity != null && !(blockEntity instanceof DecalCopycatBlockEntity)) {
            renderAttached(context, renderWorld, matrices, buffer, blockEntity);
            return;
        }
        if (!(blockEntity instanceof DecalCopycatBlockEntity be)) {
            return;
        }
        Map<Direction, DecalState> back = be.getDecals();
        Map<Direction, DecalState> front = be.getFrontDecals();
        if (!anyGlows(back) && !anyGlows(front)) {
            return;
        }
        BlockState state = context.state;
        PoseStack pose = matrices.getViewProjection();
        pose.pushPose();
        pose.last().pose().mul(matrices.getModel().last().pose());
        int worldLight = context.position == null || context.world == null ? LightTexture.FULL_BLOCK
                : LevelRenderer.getLightColor(context.world, BlockPos.containing(context.position));
        VertexConsumer consumer = buffer.getBuffer(RenderType.textPolygonOffset(TextureAtlas.LOCATION_BLOCKS));
        for (Direction face : Direction.values()) {
            DecalState backDecal = back.get(face);
            DecalState frontDecal = front.get(face);
            boolean backGlows = backDecal != null && backDecal.glow();
            boolean frontGlows = frontDecal != null && frontDecal.glow();
            DecalFaces.Window window = DecalFaces.window(state, face);
            if (!backGlows && !frontGlows || window == null) {
                continue;
            }
            BlockPos neighbour = context.localPos.relative(face);
            if (window.onBoundary() && renderWorld.getBlockState(neighbour).isSolidRender(renderWorld, neighbour)) {
                continue;
            }
            if (backGlows) {
                draw(pose, consumer, backDecal, face, window, LightTexture.FULL_BRIGHT);
            }
            if (frontDecal != null) {
                draw(pose, consumer, frontDecal, face, window, frontGlows ? LightTexture.FULL_BRIGHT : worldLight);
            }
        }
        pose.popPose();
    }

    /** The quads worked out for a Copycats+ copycat's decals, kept while its decals are the same (they rarely change). */
    private record Cached(AttachedDecals decals, BlockState state, List<AttachedDecalModel.GlowQuad> quads) {
    }

    private static final Map<BlockEntity, Cached> ATTACHED = Collections.synchronizedMap(new WeakHashMap<>());

    /** Copycats+'s copycats: their glowing decals, projected onto the copycat's own quads as its model draws them. */
    private static void renderAttached(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices,
            MultiBufferSource buffer, BlockEntity be) {
        AttachedDecals decals = be.getExistingData(DecalsAttachments.DECALS.get()).orElse(null);
        if (decals == null || !anyGlows(decals.back()) && !anyGlows(decals.front())) {
            return;
        }
        BlockState state = context.state;
        Cached cached = ATTACHED.get(be);
        if (cached == null || cached.decals() != decals || cached.state() != state) {
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            cached = new Cached(decals, state, AttachedDecalModel.glowQuads(model, renderWorld, context.localPos, state, decals));
            ATTACHED.put(be, cached);
        }
        if (cached.quads().isEmpty()) {
            return;
        }
        PoseStack pose = matrices.getViewProjection();
        pose.pushPose();
        pose.last().pose().mul(matrices.getModel().last().pose());
        int worldLight = context.position == null || context.world == null ? LightTexture.FULL_BLOCK
                : LevelRenderer.getLightColor(context.world, BlockPos.containing(context.position));
        VertexConsumer consumer = buffer.getBuffer(RenderType.textPolygonOffset(TextureAtlas.LOCATION_BLOCKS));
        for (AttachedDecalModel.GlowQuad glow : cached.quads()) {
            int rgb = glow.quad().getTintIndex() & 0xFFFFFF;
            consumer.putBulkData(pose.last(), glow.quad(), (rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f, 1f,
                    glow.glows() ? LightTexture.FULL_BRIGHT : worldLight, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
    }

    private static boolean anyGlows(Map<Direction, DecalState> decals) {
        for (DecalState decal : decals.values()) {
            if (decal.glow()) {
                return true;
            }
        }
        return false;
    }

    private static void draw(PoseStack pose, VertexConsumer consumer, DecalState decal, Direction face, DecalFaces.Window window,
            int light) {
        BakedQuad quad = DecalQuads.quad(decal, face, window);
        if (quad == null) {
            return;
        }
        int rgb = decal.rgb();
        consumer.putBulkData(pose.last(), quad, (rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f, 1f, light,
                OverlayTexture.NO_OVERLAY);
    }
}
