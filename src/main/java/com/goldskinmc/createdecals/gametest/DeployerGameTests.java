package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.block.DecalUndo;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.registry.DecalsItems;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Create's Deployer with a Decal Stencil: it stamps the decal two blocks in front of it, as a player's click would,
 * standing or carried on a contraption, and its clicks stay out of anyone's undo.
 */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeployerGameTests {
    private static final StencilData COG = new StencilData(Optional.of(Decals.id("cog")), 0xF9801D, 0, false, false, 1, 1);

    private DeployerGameTests() {
    }

    private static ItemStack stencil(StencilData data) {
        ItemStack stack = new ItemStack(DecalsItems.DECAL_STENCIL.get());
        stack.set(DecalsComponents.STENCIL.get(), data);
        return stack;
    }

    private static void hand(GameTestHelper helper, BlockPos deployer, ItemStack stack) {
        IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(deployer), null);
        helper.assertTrue(items != null, "the Deployer has no inventory");
        helper.assertTrue(items.insertItem(0, stack, false).isEmpty(), "the Deployer did not take the stencil");
    }

    private static boolean hasCog(DecalCopycatBlockEntity block, Direction face) {
        DecalState decal = block.getDecal(face);
        return decal != null && decal.id().equals(COG.decal().get()) && decal.rgb() == COG.rgb();
    }

    /** A Deployer facing east, turned by a creative motor under it, stamps the west face of the Decal Block two blocks east. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void deployerStampsDecals(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 2, 2);
        BlockPos deployer = new BlockPos(1, 2, 2);
        helper.setBlock(target, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        helper.setBlock(deployer, AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST)
                .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        hand(helper, deployer, stencil(COG));
        helper.setBlock(deployer.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        DecalCopycatBlockEntity block = helper.getBlockEntity(target);
        helper.succeedWhen(() -> {
            helper.assertTrue(hasCog(block, Direction.WEST), "no cog on the west face yet: " + block.getDecals());
            helper.assertTrue(block.getDecals().size() == 1, "stamped elsewhere too: " + block.getDecals());
        });
    }

    /**
     * A Deployer facing down on a Mechanical Piston's front, carried east one block over two Decal Blocks, stamps the tops
     * of both, the one it sets off over and the one it reaches (a moving Deployer acts on each block it passes; Create
     * hands it a level ray, so {@code DecalEdits.clickedFace} takes the top).
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void movingDeployerStampsDecals(GameTestHelper helper) {
        BlockPos start = new BlockPos(2, 1, 2);
        BlockPos end = new BlockPos(3, 1, 2);
        helper.setBlock(start, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        helper.setBlock(end, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        BlockPos piston = new BlockPos(1, 3, 2);
        BlockPos deployer = new BlockPos(2, 3, 2);
        helper.setBlock(piston.west(), AllBlocks.PISTON_EXTENSION_POLE.getDefaultState().setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(piston, AllBlocks.MECHANICAL_PISTON.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST)
                .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        helper.setBlock(deployer, AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.DOWN)
                .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        hand(helper, deployer, stencil(COG));
        helper.setBlock(piston.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        DecalCopycatBlockEntity arrived = helper.getBlockEntity(end);
        // Which way a motor's turn drives a piston depends on the axes: if it would retract an already retracted one, turn back
        helper.runAfterDelay(40, () -> {
            if (helper.getBlockState(piston).getValue(MechanicalPistonBlock.STATE) == MechanicalPistonBlock.PistonState.RETRACTED) {
                ScrollValueBehaviour speed = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(piston.below()),
                        ScrollValueBehaviour.TYPE);
                speed.setValue(-speed.getValue());
            }
        });
        DecalCopycatBlockEntity left = helper.getBlockEntity(start);
        helper.succeedWhen(() -> {
            helper.assertTrue(hasCog(left, Direction.UP) && left.getDecals().size() == 1, "the block it set off over: " + left.getDecals());
            helper.assertTrue(hasCog(arrived, Direction.UP) && arrived.getDecals().size() == 1, "the block it reached: " + arrived.getDecals());
        });
    }

    /** A Deployer with a dye recolours the decal in front of it, as a player's dye does. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void deployerDyesDecals(GameTestHelper helper) {
        BlockPos target = new BlockPos(3, 2, 2);
        BlockPos deployer = new BlockPos(1, 2, 2);
        helper.setBlock(target, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity block = helper.getBlockEntity(target);
        block.setDecal(Direction.WEST, COG.toDecal().get());
        helper.setBlock(deployer, AllBlocks.DEPLOYER.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.EAST)
                .setValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE, true));
        hand(helper, deployer, new ItemStack(Items.BLUE_DYE));
        helper.setBlock(deployer.below(), AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        int blue = DyeColor.BLUE.getTextureDiffuseColor() & 0xFFFFFF;
        helper.succeedWhen(() -> helper.assertTrue(block.getDecal(Direction.WEST) != null && (block.getDecal(Direction.WEST).rgb() & 0xFFFFFF) == blue,
                "not dyed blue: " + block.getDecals()));
    }

    /** A fake player's stencil clicks (a Deployer's) put decals on but leave no undo step, for it or its owner. */
    @GameTest(template = "empty")
    public static void fakePlayersLeaveNoUndo(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(COG));
        BlockPos absolute = helper.absolutePos(pos);
        helper.useBlock(pos, player, new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
        DecalCopycatBlockEntity block = helper.getBlockEntity(pos);
        helper.assertTrue(hasCog(block, Direction.UP), "a fake player's click put no decal on: " + block.getDecals());
        helper.assertTrue(DecalUndo.steps(player) == 0, "a fake player's click was kept for undo");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.succeed();
    }
}
