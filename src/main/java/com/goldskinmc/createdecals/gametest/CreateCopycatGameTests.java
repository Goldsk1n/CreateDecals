package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.AttachedDecalHolder;
import com.goldskinmc.createdecals.block.DecalClipboard;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.block.DecalHolder;
import com.goldskinmc.createdecals.contraption.DecalMovement;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.registry.DecalsItems;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.schematic.nbt.SafeNbtWriterRegistry;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import com.simibubi.create.content.decoration.copycat.CopycatStepBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.BlockHelper;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create's own Copycat Panel and Copycat Step take decals as they are, kept beside their block entity
 * ({@code block/AttachedCopycats}): they stay Create's blocks, with their state, material and consumed item.
 */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreateCopycatGameTests {
    private static final StencilData SEVEN = new StencilData(Optional.of(Decals.id("digit_7")), 0xB02E26, 1, false, false, 1, 1);
    private static final DecalState UPRIGHT = new DecalState(Decals.id("letter_r"), 0xFFFFFF, 0, false);

    private CreateCopycatGameTests() {
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

    private static DecalHolder holder(GameTestHelper helper, BlockPos pos) {
        return DecalHolder.at(helper.getLevel(), helper.absolutePos(pos));
    }

    private static void noDrops(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "an item dropped");
    }

    /**
     * A 2x1 decal clicked on the middle one of three Copycat Panels lands on the two it covers; all three stay Create's
     * panels with their facing, waterlogging, material and consumed item, nothing drops. Sneaking and the eraser on a bare
     * panel change nothing; a stencil on stone does nothing.
     */
    @GameTest(template = "empty")
    public static void stencilDecoratesCreatePanels(GameTestHelper helper) {
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
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(StencilData.EMPTY));
        player.setShiftKeyDown(true);
        click(helper, player, middle, Direction.SOUTH);
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(StencilData.EMPTY.withErase(true)));
        click(helper, player, middle, Direction.SOUTH);
        helper.assertTrue(holder(helper, middle).getDecal(Direction.SOUTH, false) == null, "sneaking or erasing put a decal on");

        StencilData arrow = new StencilData(Optional.of(Decals.id("arrow_2x1")), 0x3C44AA, 0, false, false, 2, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stencil(arrow));
        click(helper, player, middle, Direction.SOUTH);
        int decorated = 0;
        for (int x = 1; x <= 3; x++) {
            BlockPos pos = new BlockPos(x, 2, 2);
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(state.is(AllBlocks.COPYCAT_PANEL.get()) && state.getValue(CopycatPanelBlock.FACING) == Direction.SOUTH
                    && state.getValue(BlockStateProperties.WATERLOGGED), "not Create's panel as it was: " + state);
            CopycatBlockEntity be = helper.getBlockEntity(pos);
            helper.assertTrue(log.equals(be.getMaterial()) && be.getConsumedItem().is(Items.OAK_LOG), "material " + be.getMaterial());
            DecalState decal = holder(helper, pos).getDecal(Direction.SOUTH, false);
            if (decal != null) {
                helper.assertTrue(decal.id().equals(Decals.id("arrow_2x1")) && decal.rgb() == 0x3C44AA, "decal " + decal);
                decorated++;
            }
        }
        helper.assertTrue(decorated == 2, decorated + " panels took the 2x1 decal, not 2");
        noDrops(helper);

        BlockPos stone = new BlockPos(3, 1, 4);
        helper.setBlock(stone, Blocks.STONE);
        click(helper, player, stone, Direction.UP);
        helper.assertBlockPresent(Blocks.STONE, stone);
        helper.succeed();
    }

    /**
     * Create's Copycat Step takes a decal on any face (its top on one, its inner back face on another) and stays Create's
     * step with its facing, half, waterlogging, material and consumed item. Turned by Create's transform (as a contraption
     * or a schematic does), its top decal turns with it.
     */
    @GameTest(template = "empty")
    public static void stencilDecoratesCreateSteps(GameTestHelper helper) {
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
        click(helper, player, top, Direction.UP);
        click(helper, player, back, Direction.NORTH);
        for (Object[] check : new Object[][] {{top, Direction.UP}, {back, Direction.NORTH}}) {
            BlockPos pos = (BlockPos) check[0];
            Direction face = (Direction) check[1];
            helper.assertTrue(createStep.equals(helper.getBlockState(pos)), "not Create's step as it was: " + helper.getBlockState(pos));
            CopycatBlockEntity be = helper.getBlockEntity(pos);
            helper.assertTrue(log.equals(be.getMaterial()) && be.getConsumedItem().is(Items.OAK_LOG), "material " + be.getMaterial());
            helper.assertTrue(SEVEN.toDecal().get().equals(holder(helper, pos).getDecal(face, false)), "no decal on " + face);
        }
        noDrops(helper);

        BlockEntity turned = helper.getBlockEntity(top);
        new StructureTransform(BlockPos.ZERO, Direction.Axis.Y, Rotation.CLOCKWISE_90, Mirror.NONE).apply(turned);
        DecalState after = holder(helper, top).getDecal(Direction.UP, false);
        helper.assertTrue(after != null && after.rotation() == (SEVEN.rotation() + 1) % 4, "the top decal did not turn: " + after);
        helper.succeed();
    }

    /**
     * A Copycat Panel's decals go through everything a block entity goes through: saving and loading, Create's transform
     * (turned a quarter, its east decal ends on the south face), its schematic writer, and the Schematicannon placing it.
     */
    @GameTest(template = "empty")
    public static void createPanelDecalsSaveTurnAndPrint(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, AllBlocks.COPYCAT_PANEL.getDefaultState().setValue(CopycatPanelBlock.FACING, Direction.EAST));
        CopycatBlockEntity be = helper.getBlockEntity(pos);
        be.setMaterial(Blocks.OAK_PLANKS.defaultBlockState());
        be.setConsumedItem(new ItemStack(Items.OAK_PLANKS));
        holder(helper, pos).setDecal(Direction.EAST, false, UPRIGHT);
        holder(helper, pos).setDecal(Direction.EAST, true, UPRIGHT.withRgb(0x3C44AA));

        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = be.saveWithFullMetadata(registries);
        BlockEntity loaded = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), saved, registries);
        helper.assertTrue(loaded != null && UPRIGHT.equals(new AttachedDecalHolder(loaded).getDecal(Direction.EAST, false))
                && new AttachedDecalHolder(loaded).getDecal(Direction.EAST, true) != null, "decals lost in saving: " + saved);

        var writer = SafeNbtWriterRegistry.REGISTRY.get(be.getType());
        helper.assertTrue(writer != null, "no schematic writer for Create's copycats");
        CompoundTag schematic = new CompoundTag();
        writer.writeSafe(be, schematic, registries);
        BlockPos printed = new BlockPos(3, 1, 3);
        BlockHelper.placeSchematicBlock(helper.getLevel(), be.getBlockState(), helper.absolutePos(printed),
                new ItemStack(AllBlocks.COPYCAT_PANEL.get()), schematic);
        DecalHolder print = holder(helper, printed);
        helper.assertTrue(print != null && UPRIGHT.equals(print.getDecal(Direction.EAST, false))
                        && helper.<CopycatBlockEntity>getBlockEntity(printed).getMaterial().is(Blocks.OAK_PLANKS),
                "the Schematicannon placed " + (print != null ? print.getDecal(Direction.EAST, false) : "no holder") + " from " + schematic);

        new StructureTransform(BlockPos.ZERO, Direction.Axis.Y, Rotation.CLOCKWISE_90, Mirror.NONE).apply(be);
        helper.assertTrue(UPRIGHT.equals(holder(helper, pos).getDecal(Direction.SOUTH, false))
                && holder(helper, pos).getDecal(Direction.EAST, false) == null, "the east decal should be on the south face");
        helper.succeed();
    }

    /**
     * Create's Clipboard copies a Decal Block's face onto a Copycat Panel, through the clipboard behaviour the panel is
     * given when a Clipboard comes near it ({@code DecalClipboard}); and both of Create's copycats have our movement
     * behaviour, which redraws glowing decals full-bright on contraptions.
     */
    @GameTest(template = "empty")
    public static void clipboardAndGlowOnCreateCopycats(GameTestHelper helper) {
        BlockPos from = new BlockPos(1, 1, 2);
        BlockPos to = new BlockPos(3, 1, 2);
        helper.setBlock(from, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        helper.setBlock(to, AllBlocks.COPYCAT_PANEL.getDefaultState().setValue(CopycatPanelBlock.FACING, Direction.SOUTH));
        DecalCopycatBlockEntity source = helper.getBlockEntity(from);
        DecalState decal = SEVEN.toDecal().get();
        source.setDecal(Direction.NORTH, decal);
        var registries = helper.getLevel().registryAccess();
        CompoundTag clipboard = new CompoundTag();
        helper.assertTrue(source.writeToClipboard(registries, clipboard, Direction.NORTH), "nothing copied");

        BlockEntity panel = helper.getBlockEntity(to);
        DecalClipboard.attachIfHeld(AllBlocks.CLIPBOARD.asStack(), panel);
        DecalClipboard.Behaviour behaviour = ((SmartBlockEntity) panel).getBehaviour(DecalClipboard.Behaviour.TYPE);
        helper.assertTrue(behaviour != null, "Create's panel got no clipboard behaviour of ours");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(behaviour.readFromClipboard(registries, clipboard, player, Direction.SOUTH, false), "not pasted");
        helper.assertTrue(decal.equals(holder(helper, to).getDecal(Direction.SOUTH, false)), "pasted " + holder(helper, to).getDecal(Direction.SOUTH, false));

        for (var block : new net.minecraft.world.level.block.Block[] {AllBlocks.COPYCAT_PANEL.get(), AllBlocks.COPYCAT_STEP.get()}) {
            helper.assertTrue(MovementBehaviour.REGISTRY.get(block) instanceof DecalMovement,
                    block + " should have our movement behaviour: " + MovementBehaviour.REGISTRY.get(block));
        }
        helper.succeed();
    }
}
