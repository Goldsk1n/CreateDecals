package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.AttachedDecalHolder;
import com.goldskinmc.createdecals.block.DecalHolder;
import com.goldskinmc.createdecals.block.DecalUndo;
import com.goldskinmc.createdecals.compat.CopycatsPlus;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsAttachments;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.registry.DecalsItems;
import com.goldskinmc.createdecals.stencil.StencilData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Decals on Copycats+'s copycats, through the server's own handling of a player's right click (so the click events run as
 * in play). They pass at once without Copycats+; run them with {@code -Pdev_copycats=true}.
 */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CopycatsPlusGameTests {
    private static final StencilData STRIPE = new StencilData(Optional.of(Decals.id("stripe")), 0xF9801D, 0, false, false, 1, 1);
    private static final StencilData SEVEN = new StencilData(Optional.of(Decals.id("digit_7")), 0x1D1D21, 0, false, false, 1, 1);

    private CopycatsPlusGameTests() {
    }

    private static ItemStack stencil(StencilData data) {
        ItemStack stack = new ItemStack(DecalsItems.DECAL_STENCIL.get());
        stack.set(DecalsComponents.STENCIL.get(), data);
        return stack;
    }

    private static Block copycat(String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(CopycatsPlus.MOD_ID, name));
    }

    /** A right click as the server handles one from a player, events included. */
    private static void click(GameTestHelper helper, FakePlayer player, ItemStack held, BlockPos pos, Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false));
    }

    /**
     * A slope, a stair and a full copycat of Copycats+ each take a stripe from the stencil on their top, and a 7 on the
     * front layer of the slope; saving and loading keeps them (the attachment rides the block entity's data, as
     * contraptions and Sable vehicles carry it); dye recolours, the eraser takes the 7 off and undo puts it back.
     */
    @GameTest(template = "empty")
    public static void copycatsPlusTakeDecals(GameTestHelper helper) {
        if (!CopycatsPlus.loaded()) {
            helper.succeed();
            return;
        }
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 3, 2))));
        String[] names = {"copycat_slope", "copycat_stairs", "copycat_block"};
        for (int i = 0; i < names.length; i++) {
            BlockPos pos = new BlockPos(1 + i, 1, 2);
            Block block = copycat(names[i]);
            helper.assertTrue(block != null && CopycatsPlus.takesDecals(block.defaultBlockState()), names[i] + " should take decals");
            helper.setBlock(pos, block.defaultBlockState());
            click(helper, player, stencil(STRIPE), pos, Direction.UP);
            DecalHolder holder = DecalHolder.at(helper.getLevel(), helper.absolutePos(pos));
            helper.assertTrue(holder instanceof AttachedDecalHolder && STRIPE.toDecal().get().equals(holder.getDecal(Direction.UP, false)),
                    names[i] + ": " + (holder != null ? holder.getDecal(Direction.UP, false) : "no holder"));
        }

        BlockPos slope = new BlockPos(1, 1, 2);
        click(helper, player, stencil(SEVEN.withFront(true)), slope, Direction.UP);
        BlockEntity be = helper.getBlockEntity(slope);
        DecalHolder holder = DecalHolder.at(helper.getLevel(), helper.absolutePos(slope));
        helper.assertTrue(SEVEN.toDecal().get().equals(holder.getDecal(Direction.UP, true)), "front 7: " + holder.getDecal(Direction.UP, true));

        // Saved and loaded with the block entity, as a chunk, a contraption or a Sable vehicle does
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag saved = be.saveWithFullMetadata(registries);
        BlockEntity loaded = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), saved, registries);
        helper.assertTrue(loaded != null && loaded.getExistingData(DecalsAttachments.DECALS.get())
                        .map(d -> STRIPE.toDecal().get().equals(d.get(Direction.UP, false)) && SEVEN.toDecal().get().equals(d.get(Direction.UP, true)))
                        .orElse(false),
                "decals lost in saving: " + saved);

        // Dye recolours the decal on top (the 7), through the click event
        click(helper, player, new ItemStack(Items.LIME_DYE), slope, Direction.UP);
        DecalState dyed = holder.getDecal(Direction.UP, true);
        helper.assertTrue(dyed != null && dyed.rgb() == (DyeColor.LIME.getTextureDiffuseColor() & 0xFFFFFF), "dyed 7: " + dyed);

        // The eraser on the front layer takes the 7; undo puts it back
        int steps = DecalUndo.steps(player);
        click(helper, player, stencil(StencilData.EMPTY.withErase(true).withFront(true)), slope, Direction.UP);
        helper.assertTrue(holder.getDecal(Direction.UP, true) == null && holder.getDecal(Direction.UP, false) != null,
                "the eraser should take the 7 only: " + holder.getDecal(Direction.UP, true) + " / " + holder.getDecal(Direction.UP, false));
        helper.assertTrue(DecalUndo.steps(player) == steps + 1, "erasing is one undo step");
        DecalUndo.Result undone = DecalUndo.undo(player, player.getMainHandItem());
        helper.assertTrue(undone != null && undone.restored() == 1 && dyed.equals(holder.getDecal(Direction.UP, true)),
                "undo: " + undone + ", " + holder.getDecal(Direction.UP, true));
        helper.succeed();
    }

    /**
     * Through Create's own code: a contraption putting a Copycats+ block back turned a quarter clockwise moves its north
     * decal to the east face, still upright, and turns its top decal (StructureTransform.apply, which asks Create's
     * transform registry first); and the schematic writer Create asks first for that type keeps the decals, so
     * a block loaded from that data has them.
     */
    @GameTest(template = "empty")
    public static void copycatsPlusDecalsTurnAndGoIntoSchematics(GameTestHelper helper) {
        if (!CopycatsPlus.loaded()) {
            helper.succeed();
            return;
        }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, copycat("copycat_block").defaultBlockState());
        BlockEntity be = helper.getBlockEntity(pos);
        DecalHolder holder = DecalHolder.at(helper.getLevel(), helper.absolutePos(pos));
        DecalState upright = new DecalState(Decals.id("letter_r"), 0xFFFFFF, 0, false);
        holder.setDecal(Direction.NORTH, false, upright);
        holder.setDecal(Direction.UP, true, upright);

        new com.simibubi.create.content.contraptions.StructureTransform(BlockPos.ZERO, Direction.Axis.Y,
                net.minecraft.world.level.block.Rotation.CLOCKWISE_90, net.minecraft.world.level.block.Mirror.NONE).apply(be);
        helper.assertTrue(upright.equals(holder.getDecal(Direction.EAST, false)) && holder.getDecal(Direction.NORTH, false) == null,
                "the north decal should be on the east face: north " + holder.getDecal(Direction.NORTH, false) + ", east "
                        + holder.getDecal(Direction.EAST, false));
        helper.assertTrue(upright.withTurn(1, false).equals(holder.getDecal(Direction.UP, true)),
                "the top decal should turn a quarter: " + holder.getDecal(Direction.UP, true));

        // What Create's schematic code does first for a block entity: its type's registered safe writer
        var writer = com.simibubi.create.api.schematic.nbt.SafeNbtWriterRegistry.REGISTRY.get(be.getType());
        CompoundTag schematic = writer == null ? null : new CompoundTag();
        if (writer != null) {
            writer.writeSafe(be, schematic, helper.getLevel().registryAccess());
        }
        BlockEntity placed = schematic == null ? null
                : BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), schematic, helper.getLevel().registryAccess());
        helper.assertTrue(placed != null && placed.getExistingData(DecalsAttachments.DECALS.get())
                        .map(d -> upright.equals(d.get(Direction.EAST, false))).orElse(false),
                "schematic data lost the decals: " + schematic);
        helper.succeed();
    }

    /**
     * Create's Clipboard, through its own click handlers: R-click on a Copycats+ slope's top copies its stripe and 7, L-click
     * pastes them onto another slope and onto our Decal Block. And Copycats+'s copycats get our movement behaviour (the
     * full-bright redraw of glowing decals on contraptions), except where Copycats+ has its own.
     */
    @GameTest(template = "empty")
    public static void clipboardAndGlowOnCopycatsPlus(GameTestHelper helper) {
        if (!CopycatsPlus.loaded()) {
            helper.succeed();
            return;
        }
        FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 3, 2))));
        BlockPos from = new BlockPos(1, 1, 2);
        BlockPos to = new BlockPos(2, 1, 2);
        BlockPos ours = new BlockPos(3, 1, 2);
        helper.setBlock(from, copycat("copycat_slope").defaultBlockState());
        helper.setBlock(to, copycat("copycat_slope").defaultBlockState());
        helper.setBlock(ours, com.goldskinmc.createdecals.registry.DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalHolder source = DecalHolder.at(helper.getLevel(), helper.absolutePos(from));
        source.setDecal(Direction.UP, false, STRIPE.toDecal().get());
        source.setDecal(Direction.UP, true, SEVEN.toDecal().get());

        ItemStack clipboard = com.simibubi.create.AllBlocks.CLIPBOARD.asStack();
        click(helper, player, clipboard, from, Direction.UP);
        for (BlockPos target : new BlockPos[] {to, ours}) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(
                    player, helper.absolutePos(target), Direction.UP,
                    net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START));
            DecalHolder pasted = DecalHolder.at(helper.getLevel(), helper.absolutePos(target));
            helper.assertTrue(pasted != null && STRIPE.toDecal().get().equals(pasted.getDecal(Direction.UP, false))
                            && SEVEN.toDecal().get().equals(pasted.getDecal(Direction.UP, true)),
                    "pasted onto " + target + ": " + (pasted != null ? pasted.getDecal(Direction.UP, false) + " / " + pasted.getDecal(Direction.UP, true)
                            : "no holder") + ", clipboard " + player.getMainHandItem().getComponents());
        }

        var behaviour = com.simibubi.create.api.behaviour.movement.MovementBehaviour.REGISTRY.get(copycat("copycat_slope"));
        helper.assertTrue(behaviour instanceof com.goldskinmc.createdecals.contraption.DecalMovement,
                "a Copycats+ slope should have our movement behaviour: " + behaviour);
        helper.succeed();
    }

    /** Create's own copycats are still converted to ours, not given an attachment, with Copycats+ installed. */
    @GameTest(template = "empty")
    public static void createCopycatsStillConvert(GameTestHelper helper) {
        if (!CopycatsPlus.loaded()) {
            helper.succeed();
            return;
        }
        helper.assertFalse(CopycatsPlus.takesDecals(com.simibubi.create.AllBlocks.COPYCAT_PANEL.getDefaultState()),
                "Create's Copycat Panel is not Copycats+'s");
        helper.assertFalse(CopycatsPlus.takesDecals(com.goldskinmc.createdecals.registry.DecalsBlocks.DECAL_BLOCK.getDefaultState()),
                "our Decal Block is not Copycats+'s");
        helper.assertFalse(CopycatsPlus.takesDecals(copycat("copycat_shaft").defaultBlockState()), "a turning copycat shaft takes none");
        helper.succeed();
    }
}
