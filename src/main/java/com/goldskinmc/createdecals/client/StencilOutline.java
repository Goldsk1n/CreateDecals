package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.CreateCopycats;
import com.goldskinmc.createdecals.block.AttachedDecalHolder;
import com.goldskinmc.createdecals.block.DecalHolder;
import com.goldskinmc.createdecals.block.DecalTiles;
import com.goldskinmc.createdecals.decal.DecalFrame;
import com.goldskinmc.createdecals.decal.DecalPlacement;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

/**
 * While a stencil is held and aimed at a block, shows what a click would do there. On a decal block: a ghost of the decal
 * (or word) where each tile would land, in its colour, turn and flip, outlined green; red outlines where a tile cannot
 * land (no decal block, or a panel's side). With the eraser, the decal it would take off, outlined orange and drawn over
 * in red, so with two layers on a face it shows which one goes. On any other block, a red outline on the face: decals do
 * not go there.
 */
@EventBusSubscriber(modid = CreateDecals.MOD_ID, value = Dist.CLIENT)
public final class StencilOutline {
    /** How opaque a ghost is. */
    private static final float GHOST_ALPHA = 0.55f;
    /** How far a ghost stands off the face, so it is drawn over the decal already there. */
    private static final double GHOST_LIFT = 0.006;
    /** The eraser's mark over the decal it would take. */
    private static final int ERASE_RGB = 0xFF3A2A;
    private static final int VERTEX_INTS = DefaultVertexFormat.BLOCK.getVertexSize() / 4;

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
        if (player == null || level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        ItemStack stack = stencilIn(player);
        if (stack.isEmpty()) {
            return;
        }
        StencilData data = DecalStencilItem.data(stack);
        if (!data.erase() && !data.writes() && data.toDecal().isEmpty()) {
            return;
        }
        BlockPos clickedPos = hit.getBlockPos();
        Direction face = hit.getDirection();
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        BlockState clickedState = level.getBlockState(clickedPos);
        DecalHolder clicked = DecalHolder.at(level, clickedPos);
        if (clicked == null && !CreateCopycats.isCreateCopycat(clickedState)) {
            // Not a block that takes decals: say so before the click, on the face aimed at
            if (!data.erase()) {
                VertexConsumer lines = buffers.getBuffer(RenderType.lines());
                AABB box = clickedState.getShape(level, clickedPos).isEmpty() ? new AABB(clickedPos)
                        : clickedState.getShape(level, clickedPos).bounds().move(clickedPos);
                LevelRenderer.renderLineBox(pose, lines, faceOf(box, face).move(-camera.x, -camera.y, -camera.z), 1f, 0.25f, 0.25f, 1f);
                buffers.endBatch(RenderType.lines());
            }
            return;
        }
        List<DecalPlacement.Tile> tiles;
        @Nullable Boolean erasedLayer = null;
        if (data.erase()) {
            // The decal in the stencil's layer (or the other one) is the one the eraser takes
            erasedLayer = clicked != null ? clicked.layerFor(face, data.front()) : null;
            if (erasedLayer == null) {
                return;
            }
            DecalState shown = clicked.getDecal(face, erasedLayer);
            tiles = DecalPlacement.tiles(DecalPlacement.anchorOf(clickedPos, face, shown), face, shown);
        } else if (data.writes()) {
            tiles = DecalTiles.textTiles(clickedPos, face, data.text(), data.rgb(), data.rotation(), false, data.glow());
        } else {
            Optional<DecalState> decal = data.toDecal();
            tiles = DecalPlacement.tiles(clickedPos, face, decal.get());
        }

        // The ghosts first (see-through, drawn over the face), then the outlines over them
        VertexConsumer ghosts = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        for (DecalPlacement.Tile tile : tiles) {
            BlockState state = level.getBlockState(tile.pos());
            if (data.erase()) {
                // Only the tiles the eraser takes: the same decal, the same tile of it, in that layer
                DecalHolder holder = DecalHolder.at(level, tile.pos());
                DecalState there = holder != null ? holder.getDecal(face, erasedLayer) : null;
                if (there != null && there.sameDecalAs(tile.decal()) && there.tileX() == tile.decal().tileX()
                        && there.tileY() == tile.decal().tileY()) {
                    ghost(pose, ghosts, level, tile.pos(), state, face, there.withGlow(false), camera, ERASE_RGB, 0.7f);
                }
            } else if (lands(level, tile.pos(), face)) {
                ghost(pose, ghosts, level, tile.pos(), state, face, tile.decal(), camera, tile.decal().rgb(), GHOST_ALPHA);
            }
        }
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (DecalPlacement.Tile tile : tiles) {
            AABB box = faceBox(level, level.getBlockState(tile.pos()), tile.pos(), face).move(-camera.x, -camera.y, -camera.z);
            if (data.erase()) {
                LevelRenderer.renderLineBox(pose, lines, box, 1f, 0.55f, 0.1f, 1f);
            } else if (lands(level, tile.pos(), face)) {
                // On Copycats+'s shapes (slopes) a box would float off the face: the ghost alone shows where it goes
                if (!(DecalHolder.at(level, tile.pos()) instanceof AttachedDecalHolder)) {
                    LevelRenderer.renderLineBox(pose, lines, box, 0.3f, 1f, 0.3f, 1f);
                }
            } else {
                LevelRenderer.renderLineBox(pose, lines, box, 1f, 0.25f, 0.25f, 1f);
            }
        }
        buffers.endBatch(RenderType.lines());
    }

    /** Whether a tile would land on that face: a decal block's free face, or Create's copycat that converts. */
    private static boolean lands(Level level, BlockPos pos, Direction face) {
        DecalHolder holder = DecalHolder.at(level, pos);
        return holder != null ? holder.acceptsDecalOn(face)
                : CreateCopycats.convertible(level.getBlockState(pos), face);
    }

    /**
     * The decal's own quad for that face ({@link DecalQuads}, so the ghost matches what the click makes, steps' windows
     * included), lifted a little off the face and drawn see-through in {@code rgb}; full-bright if the decal glows.
     */
    private static void ghost(PoseStack pose, VertexConsumer out, Level level, BlockPos pos, BlockState state, Direction face,
            DecalState decal, Vec3 camera, int rgb, float alpha) {
        if (DecalHolder.at(level, pos) instanceof AttachedDecalHolder) {
            // Copycats+: the decal projected onto the copycat's own quads, as its model will draw it
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            int light = decal.glow() ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(level, pos.relative(face));
            for (BakedQuad quad : AttachedDecalModel.ghostQuads(model, level, pos, state, decal, face)) {
                emit(pose, out, quad, pos, face, camera, rgb, alpha, light);
            }
            return;
        }
        DecalFaces.Window window = DecalFaces.window(state, face);
        BakedQuad quad = DecalQuads.quad(decal, face, window != null ? window : DecalFaces.Window.FULL);
        if (quad == null) {
            return;
        }
        int light = decal.glow() ? LightTexture.FULL_BRIGHT
                : LevelRenderer.getLightColor(level, window == null || window.onBoundary() ? pos.relative(face) : pos);
        emit(pose, out, quad, pos, face, camera, rgb, alpha, light);
    }

    private static void emit(PoseStack pose, VertexConsumer out, BakedQuad quad, BlockPos pos, Direction face, Vec3 camera, int rgb,
            float alpha, int light) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        double ox = pos.getX() - camera.x + face.getStepX() * GHOST_LIFT;
        double oy = pos.getY() - camera.y + face.getStepY() * GHOST_LIFT;
        double oz = pos.getZ() - camera.z + face.getStepZ() * GHOST_LIFT;
        PoseStack.Pose last = pose.last();
        int[] vertices = quad.getVertices();
        for (int i = 0; i < 4; i++) {
            int at = i * VERTEX_INTS;
            out.addVertex(last, (float) (ox + Float.intBitsToFloat(vertices[at])), (float) (oy + Float.intBitsToFloat(vertices[at + 1])),
                            (float) (oz + Float.intBitsToFloat(vertices[at + 2])))
                    .setColor(r, g, b, alpha)
                    .setUv(Float.intBitsToFloat(vertices[at + 4]), Float.intBitsToFloat(vertices[at + 5]))
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(last, face.getStepX(), face.getStepY(), face.getStepZ());
        }
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

    /** The thin slab of {@code box} at its {@code face} side. */
    private static AABB faceOf(AABB box, Direction face) {
        double t = 0.004;
        return switch (face) {
            case DOWN -> new AABB(box.minX, box.minY - t, box.minZ, box.maxX, box.minY + t, box.maxZ);
            case UP -> new AABB(box.minX, box.maxY - t, box.minZ, box.maxX, box.maxY + t, box.maxZ);
            case NORTH -> new AABB(box.minX, box.minY, box.minZ - t, box.maxX, box.maxY, box.minZ + t);
            case SOUTH -> new AABB(box.minX, box.minY, box.maxZ - t, box.maxX, box.maxY, box.maxZ + t);
            case WEST -> new AABB(box.minX - t, box.minY, box.minZ, box.minX + t, box.maxY, box.maxZ);
            case EAST -> new AABB(box.maxX - t, box.minY, box.minZ, box.maxX + t, box.maxY, box.maxZ);
        };
    }

    /**
     * A thin box on the face where the decal lies: a panel's front 3/16 out from its back, a step's face where it is
     * (DecalFaces), else the block's face. Create's step not yet converted has the same faces as ours.
     */
    private static AABB faceBox(Level level, BlockState state, BlockPos pos, Direction face) {
        if (DecalHolder.at(level, pos) instanceof AttachedDecalHolder) {
            // Copycats+: the face of the copycat's outline box
            VoxelShape shape = state.getShape(level, pos);
            return faceOf(shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos), face);
        }
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
