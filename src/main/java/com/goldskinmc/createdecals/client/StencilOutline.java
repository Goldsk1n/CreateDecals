package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.CreateCopycats;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.block.DecalTiles;
import com.goldskinmc.createdecals.decal.DecalFrame;
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
                || !(level.getBlockEntity(hit.getBlockPos()) instanceof DecalCopycatBlockEntity
                        || CreateCopycats.isCreateCopycat(level.getBlockState(hit.getBlockPos())))) {
            return;
        }
        DecalCopycatBlockEntity clicked = level.getBlockEntity(hit.getBlockPos()) instanceof DecalCopycatBlockEntity ours ? ours : null;
        ItemStack stack = stencilIn(player);
        if (stack.isEmpty()) {
            return;
        }
        StencilData data = DecalStencilItem.data(stack);
        Direction face = hit.getDirection();
        List<DecalPlacement.Tile> tiles;
        if (data.erase()) {
            // The decal in the stencil's layer (or the other one) is the one the eraser takes
            Boolean front = clicked != null ? clicked.layerFor(face, data.front()) : null;
            if (front == null) {
                return;
            }
            DecalState shown = clicked.getDecal(face, front);
            tiles = DecalPlacement.tiles(DecalPlacement.anchorOf(hit.getBlockPos(), face, shown), face, shown);
        } else if (data.writes()) {
            tiles = DecalTiles.textTiles(hit.getBlockPos(), face, data.text(), data.rgb(), data.rotation(), false, data.glow());
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
            boolean lands = level.getBlockEntity(tile.pos()) instanceof DecalCopycatBlockEntity be ? be.acceptsDecalOn(face)
                    : CreateCopycats.convertible(level.getBlockState(tile.pos()), face);
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

    /**
     * A thin box on the face where the decal lies: a panel's front 3/16 out from its back, a step's face where it is
     * (DecalFaces), else the block's face. Create's step not yet converted has the same faces as ours.
     */
    private static AABB faceBox(BlockState state, BlockPos pos, Direction face) {
        DecalFaces.Window window = DecalFaces.window(state, face);
        if (window == null) {
            window = DecalFaces.Window.FULL;
        }
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 right = Vec3.atLowerCornerOf(DecalFrame.right(face).getNormal());
        Vec3 up = Vec3.atLowerCornerOf(DecalFrame.up(face).getNormal());
        Vec3 centre = Vec3.atCenterOf(pos).add(normal.scale(window.depth() - 0.5 + 0.004));
        Vec3 a = centre.add(right.scale(window.s0() - 0.5)).add(up.scale(0.5 - window.t0())).add(normal.scale(-0.004));
        Vec3 b = centre.add(right.scale(window.s1() - 0.5)).add(up.scale(0.5 - window.t1())).add(normal.scale(0.004));
        return new AABB(a, b);
    }
}
