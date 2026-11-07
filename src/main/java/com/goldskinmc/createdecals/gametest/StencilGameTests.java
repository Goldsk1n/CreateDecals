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
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import com.simibubi.create.content.decoration.copycat.CopycatStepBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.Half;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Decal Stencil and Create's Clipboard, through the same calls a player's click makes. */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StencilGameTests {
    private static final StencilData SEVEN = new StencilData(Optional.of(Decals.id("digit_7")), 0xB02E26, 1, false, false, 1, 1);

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

    /**
     * Create's own Copycat Panel takes a decal by becoming a Decal Panel: a 2x1 decal clicked on the middle one of three
     * converts the two it covers, keeping facing, waterlogging, material and the consumed item (nothing drops), and leaves
     * the third Create's. A side of Create's panel, sneaking and the eraser change nothing.
     */
    /**
     * Create's own Copycat Step takes a decal on any face by becoming a Decal Step: its top face on one, its inner back face
     * on another, keeping facing, half, waterlogging, material and the consumed item (nothing drops). Then a step's decal
     * turns with it on a contraption, as a block's does.
     */
    @GameTest(template = "empty")
    public static void stencilConvertsCreateSteps(GameTestHelper helper) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState createStep = AllBlocks.COPYCAT_STEP.getDefaultState().setValue(CopycatStepBlock.FACING, Direction.SOUTH)
                .setValue(CopycatStepBlock.HALF, Half.TOP).setValue(BlockStateProperties.WATERLOGGED, true);
        BlockPos top = new BlockPos(1, 2, 2);
        BlockPos back = new BlockPos(3, 2, 2);
        for (BlockPos pos : new BlockPos[] {top, back}) {
            helper.setBlock(pos, createStep);
            CopycatBlockEntity be = helper.getBlockEntity(pos);
            be.setMaterial(log);
            be.setConsumedItem(new ItemStack(Items.OAK_LOG));
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(SEVEN));
        // Any face takes a decal: the top of one step, the inner back face of the other
        click(helper, player, top, Direction.UP);
        click(helper, player, back, Direction.NORTH);
        for (Object[] check : new Object[][] {{top, Direction.UP}, {back, Direction.NORTH}}) {
            BlockPos pos = (BlockPos) check[0];
            Direction face = (Direction) check[1];
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(state.is(DecalsBlocks.DECAL_STEP.get()), "not converted: " + state);
            helper.assertTrue(state.getValue(CopycatStepBlock.FACING) == Direction.SOUTH && state.getValue(CopycatStepBlock.HALF) == Half.TOP
                    && state.getValue(BlockStateProperties.WATERLOGGED), "state not kept: " + state);
            DecalCopycatBlockEntity be = helper.getBlockEntity(pos);
            helper.assertTrue(log.equals(be.getMaterial()) && be.getConsumedItem().is(Items.OAK_LOG), "material " + be.getMaterial());
            helper.assertTrue(SEVEN.toDecal().get().equals(be.getDecal(face)), "decal on " + face + ": " + be.getDecals());
        }
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "an item dropped");

        // Turned on a contraption, a step's decals turn with it like a block's
        helper.getLevel().setBlock(helper.absolutePos(top), helper.getBlockState(top).rotate(Rotation.CLOCKWISE_90), 2 | 16);
        DecalCopycatBlockEntity turned = helper.getBlockEntity(top);
        turned.transform(turned, new StructureTransform(BlockPos.ZERO, Direction.Axis.Y, Rotation.CLOCKWISE_90, Mirror.NONE));
        helper.assertTrue(turned.getDecal(Direction.UP) != null && turned.getDecal(Direction.UP).rotation() == (SEVEN.rotation() + 1) % 4,
                "the top decal did not turn: " + turned.getDecals());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stencilConvertsCreatePanels(GameTestHelper helper) {
        BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState createPanel = AllBlocks.COPYCAT_PANEL.getDefaultState().setValue(CopycatPanelBlock.FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.WATERLOGGED, true);
        for (int x = 1; x <= 3; x++) {
            BlockPos pos = new BlockPos(x, 2, 2);
            helper.setBlock(pos, createPanel);
            CopycatBlockEntity be = helper.getBlockEntity(pos);
            be.setMaterial(log);
            be.setConsumedItem(new ItemStack(Items.OAK_LOG));
        }
        BlockPos middle = new BlockPos(2, 2, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        player.setShiftKeyDown(true);
        click(helper, player, middle, Direction.SOUTH);
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(StencilData.EMPTY.withErase(true)));
        click(helper, player, middle, Direction.SOUTH);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(SEVEN));
        click(helper, player, middle, Direction.EAST);
        helper.assertBlockPresent(AllBlocks.COPYCAT_PANEL.get(), middle);

        StencilData arrow = new StencilData(Optional.of(Decals.id("arrow_2x1")), 0x3C44AA, 0, false, false, 2, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(arrow));
        click(helper, player, middle, Direction.SOUTH);
        int converted = 0;
        for (int x = 1; x <= 3; x++) {
            BlockPos pos = new BlockPos(x, 2, 2);
            BlockState state = helper.getBlockState(pos);
            if (!state.is(DecalsBlocks.DECAL_PANEL.get())) {
                helper.assertBlockPresent(AllBlocks.COPYCAT_PANEL.get(), pos);
                continue;
            }
            converted++;
            helper.assertTrue(state.getValue(CopycatPanelBlock.FACING) == Direction.SOUTH
                    && state.getValue(BlockStateProperties.WATERLOGGED), "state not kept: " + state);
            DecalCopycatBlockEntity be = helper.getBlockEntity(pos);
            helper.assertTrue(log.equals(be.getMaterial()), "material " + be.getMaterial());
            helper.assertTrue(be.getConsumedItem().is(Items.OAK_LOG), "consumed item " + be.getConsumedItem());
            DecalState decal = be.getDecal(Direction.SOUTH);
            helper.assertTrue(decal != null && decal.id().equals(Decals.id("arrow_2x1")) && decal.rgb() == 0x3C44AA, "decal " + decal);
        }
        helper.assertTrue(converted == 2, converted + " panels converted, not 2");
        helper.assertBlockPresent(DecalsBlocks.DECAL_PANEL.get(), middle);
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "an item dropped");
        helper.succeed();
    }
}
