package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalFrame;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.registry.DecalsItems;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Two decals on one face (back and front), typed words and glowing decals, through the calls a player's click makes. */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LayerGameTests {
    private static final StencilData STRIPE = new StencilData(Optional.of(Decals.id("stripe")), 0xF9801D, 0, false, false, 1, 1);
    private static final StencilData SEVEN = new StencilData(Optional.of(Decals.id("digit_7")), 0x1D1D21, 0, false, false, 1, 1);

    private LayerGameTests() {
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
     * A stripe on the back layer and a 7 on the front of one face; both kept by saving and by the Clipboard. Dye, pick-up
     * and the eraser take the front decal first: the eraser peels the 7, then the stripe.
     */
    @GameTest(template = "empty")
    public static void twoLayersOnOneFace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        Direction face = Direction.SOUTH;
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity block = helper.getBlockEntity(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(STRIPE));
        click(helper, player, pos, face);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(SEVEN.withFront(true)));
        click(helper, player, pos, face);
        helper.assertTrue(STRIPE.toDecal().get().equals(block.getDecal(face, false)), "back: " + block.getDecal(face, false));
        helper.assertTrue(SEVEN.toDecal().get().equals(block.getDecal(face, true)), "front: " + block.getDecal(face, true));

        // Saved and loaded: both layers come back
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = block.saveWithoutMetadata(registries);
        BlockPos copyPos = new BlockPos(3, 1, 2);
        helper.setBlock(copyPos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity copy = helper.getBlockEntity(copyPos);
        copy.loadWithComponents(saved, registries);
        helper.assertTrue(STRIPE.toDecal().get().equals(copy.getDecal(face, false)) && SEVEN.toDecal().get().equals(copy.getDecal(face, true)),
                "after loading: " + copy.getDecals() + " / " + copy.getFrontDecals());

        // The Clipboard copies both layers to another face
        CompoundTag clipboard = new CompoundTag();
        helper.assertTrue(block.writeToClipboard(registries, clipboard, face), "nothing copied");
        helper.assertTrue(copy.readFromClipboard(registries, clipboard, player, Direction.UP, false), "not pasted");
        helper.assertTrue(copy.getDecal(Direction.UP, false) != null && copy.getDecal(Direction.UP, true) != null,
                "pasted " + copy.getDecal(Direction.UP, false) + " / " + copy.getDecal(Direction.UP, true));

        // Dye recolours the front decal only
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE));
        click(helper, player, pos, face);
        helper.assertTrue(block.getDecal(face, true).rgb() != SEVEN.rgb(), "front not dyed");
        helper.assertTrue(block.getDecal(face, false).rgb() == STRIPE.rgb(), "back dyed too");

        // Picking up takes the front decal and sets the stencil to the front layer
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(StencilData.EMPTY));
        player.setShiftKeyDown(true);
        click(helper, player, pos, face);
        player.setShiftKeyDown(false);
        StencilData picked = DecalStencilItem.data(player.getMainHandItem());
        helper.assertTrue(picked.decal().equals(Optional.of(Decals.id("digit_7"))) && picked.front(), "picked " + picked);

        // The eraser peels the front decal, then the back one
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(StencilData.EMPTY.withErase(true)));
        click(helper, player, pos, face);
        helper.assertTrue(block.getDecal(face, true) == null && block.getDecal(face, false) != null, "first erase left "
                + block.getDecal(face, true) + " / " + block.getDecal(face, false));
        click(helper, player, pos, face);
        helper.assertTrue(block.getDecal(face, false) == null, "second erase left " + block.getDecal(face, false));
        helper.succeed();
    }

    /** "AB 1" from the clicked face along its right: a, b, a gap, 1; a word longer than the wall reports the letters it lost. */
    @GameTest(template = "empty")
    public static void typedWordGoesAlongTheFace(GameTestHelper helper) {
        Direction face = Direction.SOUTH;
        Direction right = DecalFrame.right(face);
        // Four blocks in a row from the start, along the face's right
        BlockPos first = right.getAxisDirection() == Direction.AxisDirection.POSITIVE ? new BlockPos(0, 1, 2) : new BlockPos(4, 1, 2);
        for (int i = 0; i < 4; i++) {
            helper.setBlock(first.relative(right, i), DecalsBlocks.DECAL_BLOCK.getDefaultState());
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(StencilData.EMPTY.withRgb(0xFFFFFF).withText("ab 1").withGlow(true)));
        click(helper, player, first, face);
        String[] expected = {"letter_a", "letter_b", null, "digit_1"};
        for (int i = 0; i < 4; i++) {
            DecalCopycatBlockEntity be = helper.getBlockEntity(first.relative(right, i));
            DecalState decal = be.getDecal(face);
            if (expected[i] == null) {
                helper.assertTrue(decal == null, "a space took " + decal);
            } else {
                helper.assertTrue(decal != null && decal.id().equals(Decals.id(expected[i])) && decal.glow() && decal.width() == 1,
                        "letter " + i + " is " + decal);
            }
        }
        helper.succeed();
    }

    /** A Glow Ink Sac makes a whole 2x1 decal glow from either tile, an Ink Sac stops it; saving keeps the glow. */
    @GameTest(template = "empty")
    public static void glowInkMakesDecalsGlow(GameTestHelper helper) {
        Direction face = Direction.SOUTH;
        Direction right = DecalFrame.right(face);
        BlockPos first = right.getAxisDirection() == Direction.AxisDirection.POSITIVE ? new BlockPos(1, 1, 2) : new BlockPos(3, 1, 2);
        BlockPos second = first.relative(right);
        helper.setBlock(first, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        helper.setBlock(second, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                stencil(new StencilData(Optional.of(Decals.id("arrow_2x1")), 0xFFFFFF, 0, false, false, 2, 1)));
        click(helper, player, first, face);
        DecalCopycatBlockEntity a = helper.getBlockEntity(first);
        DecalCopycatBlockEntity b = helper.getBlockEntity(second);
        helper.assertTrue(a.getDecal(face) != null && b.getDecal(face) != null, "arrow not laid: " + a.getDecals() + b.getDecals());

        ItemStack glowInk = new ItemStack(Items.GLOW_INK_SAC);
        player.setItemInHand(InteractionHand.MAIN_HAND, glowInk);
        click(helper, player, second, face);
        helper.assertTrue(a.getDecal(face).glow() && b.getDecal(face).glow(), "not glowing: " + a.getDecal(face) + b.getDecal(face));
        helper.assertTrue(glowInk.getCount() == 1, "the ink was used up");

        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = a.saveWithoutMetadata(registries);
        helper.assertTrue(DecalState.load(saved.getCompound("Decals").getCompound(face.getName())).glow(), "glow not saved: " + saved);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.INK_SAC));
        click(helper, player, first, face);
        helper.assertFalse(a.getDecal(face).glow() || b.getDecal(face).glow(), "still glowing");
        helper.succeed();
    }
}
