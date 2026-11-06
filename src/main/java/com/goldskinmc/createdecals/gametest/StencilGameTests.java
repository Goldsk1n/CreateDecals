package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.registry.DecalsItems;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Decal Stencil and Create's Clipboard, through the same calls a player's click makes. */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StencilGameTests {
    private static final StencilData SEVEN = new StencilData(Optional.of(Decals.id("digit_7")), 0xB02E26, 1, false, false);

    private StencilGameTests() {
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

    /** The stencil puts its decal on the clicked face of a Decal Block, sneaking picks one up, the eraser removes it. */
    @GameTest(template = "empty")
    public static void stencilAppliesPicksUpAndErases(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity block = helper.getBlockEntity(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(SEVEN));
        click(helper, player, pos, Direction.EAST);
        helper.assertTrue(SEVEN.toDecal().get().equals(block.getDecal(Direction.EAST)), "not applied to the east face: " + block.getDecals());
        helper.assertTrue(block.getDecals().size() == 1, "applied elsewhere too: " + block.getDecals());

        block.setDecal(Direction.UP, new DecalState(Decals.id("letter_a"), 0x123456, 2, true));
        ItemStack empty = stencil(StencilData.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, empty);
        player.setShiftKeyDown(true);
        click(helper, player, pos, Direction.UP);
        player.setShiftKeyDown(false);
        StencilData picked = DecalStencilItem.data(player.getMainHandItem());
        helper.assertTrue(picked.decal().equals(Optional.of(Decals.id("letter_a"))) && picked.rgb() == 0x123456
                && picked.rotation() == 2 && picked.flip(), "picked up: " + picked);

        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(StencilData.EMPTY.withErase(true)));
        click(helper, player, pos, Direction.EAST);
        helper.assertTrue(block.getDecal(Direction.EAST) == null, "the eraser left " + block.getDecal(Direction.EAST));
        helper.succeed();
    }

    /** A panel takes the stencil only on its front; a stencil on a plain block does nothing. */
    @GameTest(template = "empty")
    public static void stencilNeedsADecalFace(GameTestHelper helper) {
        BlockPos panelPos = new BlockPos(1, 1, 2);
        helper.setBlock(panelPos, DecalsBlocks.DECAL_PANEL.getDefaultState().setValue(CopycatPanelBlock.FACING, Direction.UP));
        DecalCopycatBlockEntity panel = helper.getBlockEntity(panelPos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(SEVEN));
        click(helper, player, panelPos, Direction.NORTH);
        helper.assertTrue(panel.getDecals().isEmpty(), "a panel took a decal on its side: " + panel.getDecals());
        click(helper, player, panelPos, Direction.UP);
        helper.assertTrue(panel.getDecal(Direction.UP) != null, "a panel did not take a decal on its front");

        BlockPos stone = new BlockPos(3, 1, 2);
        helper.setBlock(stone, Blocks.STONE);
        click(helper, player, stone, Direction.UP);
        helper.assertBlockPresent(Blocks.STONE, stone);
        helper.succeed();
    }

    /** Create's Clipboard copies the decal on one face and pastes it onto another (a panel's front only). */
    @GameTest(template = "empty")
    public static void clipboardCopiesDecals(GameTestHelper helper) {
        BlockPos from = new BlockPos(1, 1, 2);
        BlockPos to = new BlockPos(3, 1, 2);
        helper.setBlock(from, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        helper.setBlock(to, DecalsBlocks.DECAL_PANEL.getDefaultState().setValue(CopycatPanelBlock.FACING, Direction.SOUTH));
        DecalCopycatBlockEntity source = helper.getBlockEntity(from);
        DecalCopycatBlockEntity target = helper.getBlockEntity(to);
        DecalState decal = SEVEN.toDecal().get();
        source.setDecal(Direction.NORTH, decal);
        CompoundTag clipboard = new CompoundTag();
        var registries = helper.getLevel().registryAccess();
        helper.assertTrue(source.writeToClipboard(registries, clipboard, Direction.NORTH), "nothing copied");
        helper.assertFalse(source.writeToClipboard(registries, new CompoundTag(), Direction.EAST), "copied a bare face");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertFalse(target.readFromClipboard(registries, clipboard, player, Direction.EAST, false), "pasted onto a panel's side");
        helper.assertTrue(target.readFromClipboard(registries, clipboard, player, Direction.SOUTH, false), "not pasted onto the front");
        helper.assertTrue(decal.equals(target.getDecal(Direction.SOUTH)), "pasted " + target.getDecals());
        helper.succeed();
    }
}
