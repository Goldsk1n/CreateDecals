package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import java.util.Map;
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
    private static final float PANEL_FRONT = 3 / 16f;

    private ContraptionGlow() {
    }

    public static void render(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices,
            MultiBufferSource buffer) {
        if (!(renderWorld.getBlockEntity(context.localPos) instanceof DecalCopycatBlockEntity be)) {
            return;
        }
        Map<Direction, DecalState> back = be.getDecals();
        Map<Direction, DecalState> front = be.getFrontDecals();
        if (!anyGlows(back) && !anyGlows(front)) {
            return;
        }
        BlockState state = context.state;
        Direction panelFacing = state.getOptionalValue(CopycatPanelBlock.FACING).orElse(null);
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
            if (!backGlows && !frontGlows || panelFacing != null && face != panelFacing) {
                continue;
            }
            BlockPos neighbour = context.localPos.relative(face);
            if (panelFacing == null && renderWorld.getBlockState(neighbour).isSolidRender(renderWorld, neighbour)) {
                continue;
            }
            float depth = panelFacing != null ? PANEL_FRONT : 1;
            if (backGlows) {
                draw(pose, consumer, backDecal, face, depth, LightTexture.FULL_BRIGHT);
            }
            if (frontDecal != null) {
                draw(pose, consumer, frontDecal, face, depth, frontGlows ? LightTexture.FULL_BRIGHT : worldLight);
            }
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

    private static void draw(PoseStack pose, VertexConsumer consumer, DecalState decal, Direction face, float depth, int light) {
        BakedQuad quad = DecalQuads.quad(decal, face, depth);
        if (quad == null) {
            return;
        }
        int rgb = decal.rgb();
        consumer.putBulkData(pose.last(), quad, (rgb >> 16 & 0xFF) / 255f, (rgb >> 8 & 0xFF) / 255f, (rgb & 0xFF) / 255f, 1f, light,
                OverlayTexture.NO_OVERLAY);
    }
}
