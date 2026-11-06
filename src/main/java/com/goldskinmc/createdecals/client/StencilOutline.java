package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalPlacement;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * While a stencil is held and aimed at a decal block, outlines the faces its decal would cover from there: green where
 * a tile lands, red where it cannot (no decal block, or a panel's side). With the eraser, the decal it would take off.
 */
@EventBusSubscriber(modid = CreateDecals.MOD_ID, value = Dist.CLIENT)
public final class StencilOutline {
    private StencilOutline() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !(level.getBlockEntity(hit.getBlockPos()) instanceof DecalCopycatBlockEntity clicked)) {
            return;
        }
        ItemStack stack = stencilIn(player);
        if (stack.isEmpty()) {
            return;
        }
        StencilData data = DecalStencilItem.data(stack);
        Direction face = hit.getDirection();
        List<DecalPlacement.Tile> tiles;
        if (data.erase()) {
            DecalState shown = clicked.getDecal(face);
            if (shown == null) {
                return;
            }
            tiles = DecalPlacement.tiles(DecalPlacement.anchorOf(hit.getBlockPos(), face, shown), face, shown);
        } else {
            Optional<DecalState> decal = data.toDecal();
            if (decal.isEmpty()) {
                return;
            }
            tiles = DecalPlacement.tiles(hit.getBlockPos(), face, decal.get());
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (DecalPlacement.Tile tile : tiles) {
            boolean lands = level.getBlockEntity(tile.pos()) instanceof DecalCopycatBlockEntity be && be.acceptsDecalOn(face);
            AABB box = faceBox(level.getBlockState(tile.pos()), tile.pos(), face).move(-camera.x, -camera.y, -camera.z);
            if (data.erase()) {
                LevelRenderer.renderLineBox(pose, lines, box, 1f, 0.55f, 0.1f, 1f);
            } else if (lands) {
                LevelRenderer.renderLineBox(pose, lines, box, 0.3f, 1f, 0.3f, 1f);
            } else {
                LevelRenderer.renderLineBox(pose, lines, box, 1f, 0.25f, 0.25f, 1f);
            }
        }
        buffers.endBatch(RenderType.lines());
    }

    private static ItemStack stencilIn(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof DecalStencilItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** A thin box on the face where the decal lies: a panel's front 3/16 out from its back, else the block's face. */
    private static AABB faceBox(BlockState state, BlockPos pos, Direction face) {
        double depth = state.hasProperty(CopycatPanelBlock.FACING) && state.getValue(CopycatPanelBlock.FACING) == face ? 3 / 16d : 1;
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 centre = Vec3.atCenterOf(pos).add(normal.scale(depth - 0.5 + 0.004));
        double hx = face.getAxis() == Direction.Axis.X ? 0.004 : 0.5;
        double hy = face.getAxis() == Direction.Axis.Y ? 0.004 : 0.5;
        double hz = face.getAxis() == Direction.Axis.Z ? 0.004 : 0.5;
        return new AABB(centre.x - hx, centre.y - hy, centre.z - hz, centre.x + hx, centre.y + hy, centre.z + hz);
    }
}
