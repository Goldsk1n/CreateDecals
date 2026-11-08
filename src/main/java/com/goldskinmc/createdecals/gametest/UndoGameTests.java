package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.block.DecalUndo;
import com.goldskinmc.createdecals.decal.DecalFrame;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.registry.DecalsItems;
import com.goldskinmc.createdecals.stencil.StencilData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Undoing stencil clicks ({@link DecalUndo}), through the calls a player's click and the undo key make. */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UndoGameTests {
    private static final StencilData STRIPE = new StencilData(Optional.of(Decals.id("stripe")), 0xF9801D, 0, false, false, 1, 1);
    private static final StencilData ARROW = new StencilData(Optional.of(Decals.id("arrow_2x1")), 0xFFFFFF, 0, false, false, 2, 1);

    private UndoGameTests() {
    }

    private static ItemStack stencil(StencilData data) {
        ItemStack stack = new ItemStack(DecalsItems.DECAL_STENCIL.get());
        stack.set(DecalsComponents.STENCIL.get(), data);
        return stack;
    }

    private static void click(GameTestHelper helper, Player player, BlockPos pos, Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        helper.useBlock(pos, player, new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false));
    }

    /**
     * A stripe, then a 2x1 arrow over it and the block beside it; someone else then paints over the arrow's second tile.
     * Undo takes the arrow off the first block (the stripe comes back) and leaves the second block's new decal: 1 of 2
     * tiles. Undo again takes the stripe off; a third time there is nothing left. The eraser is undone too.
     */
    @GameTest(template = "empty")
    public static void undoPutsBackTheLastClick(GameTestHelper helper) {
        Direction face = Direction.SOUTH;
        Direction right = DecalFrame.right(face);
        BlockPos first = right.getAxisDirection() == Direction.AxisDirection.POSITIVE ? new BlockPos(1, 1, 2) : new BlockPos(3, 1, 2);
        BlockPos second = first.relative(right);
        helper.setBlock(first, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        helper.setBlock(second, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity a = helper.getBlockEntity(first);
        DecalCopycatBlockEntity b = helper.getBlockEntity(second);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stripe = stencil(STRIPE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stripe);
        click(helper, player, first, face);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(ARROW));
        click(helper, player, first, face);
        helper.assertTrue(DecalUndo.steps(player) == 2, "two clicks, steps: " + DecalUndo.steps(player));
        helper.assertTrue(a.getDecal(face) != null && a.getDecal(face).id().equals(ARROW.decal().get()), "first: " + a.getDecal(face));

        // Someone else paints over the arrow's second tile
        DecalState theirs = new DecalState(Decals.id("digit_7"), 0x1D1D21, 0, false, 0, 0, 1, 1, false);
        b.setDecal(face, false, theirs);

        DecalUndo.Result undone = DecalUndo.undo(player, player.getMainHandItem());
        helper.assertTrue(undone != null && undone.restored() == 1 && undone.total() == 2, "undo arrow: " + undone);
        helper.assertTrue(STRIPE.toDecal().get().equals(a.getDecal(face)), "the stripe should be back: " + a.getDecal(face));
        helper.assertTrue(theirs.equals(b.getDecal(face)), "their decal should stay: " + b.getDecal(face));

        undone = DecalUndo.undo(player, player.getMainHandItem());
        helper.assertTrue(undone != null && undone.restored() == 1 && undone.total() == 1, "undo stripe: " + undone);
        helper.assertTrue(a.getDecal(face) == null, "the first block should be bare: " + a.getDecal(face));
        helper.assertTrue(DecalUndo.undo(player, player.getMainHandItem()) == null, "nothing should be left to undo");

        // The eraser, undone, puts the decal back; picking up (sneaking) is no step
        player.setItemInHand(InteractionHand.MAIN_HAND, stripe);
        click(helper, player, first, face);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(StencilData.EMPTY.withErase(true)));
        click(helper, player, first, face);
        helper.assertTrue(a.getDecal(face) == null, "erased: " + a.getDecal(face));
        player.setShiftKeyDown(true);
        click(helper, player, second, face);
        player.setShiftKeyDown(false);
        helper.assertTrue(DecalUndo.steps(player) == 2, "place and erase, not the pick-up: " + DecalUndo.steps(player));
        undone = DecalUndo.undo(player, player.getMainHandItem());
        helper.assertTrue(undone != null && undone.restored() == 1 && STRIPE.toDecal().get().equals(a.getDecal(face)),
                "undoing the eraser: " + undone + ", " + a.getDecal(face));
        helper.succeed();
    }

    /** At most {@link DecalUndo#STEPS} clicks are kept: the oldest go first. */
    @GameTest(template = "empty")
    public static void undoKeepsTenClicks(GameTestHelper helper) {
        Direction face = Direction.SOUTH;
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        for (int i = 0; i < DecalUndo.STEPS + 3; i++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stencil(STRIPE.withRgb(0x101010 * (i + 1))));
            click(helper, player, pos, face);
        }
        helper.assertTrue(DecalUndo.steps(player) == DecalUndo.STEPS, "steps kept: " + DecalUndo.steps(player));
        helper.succeed();
    }
}
