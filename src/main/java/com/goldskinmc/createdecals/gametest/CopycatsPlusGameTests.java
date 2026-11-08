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
