package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Headless game tests, run by {@code ./gradlew runGameTestServer}: decals are kept through saving, schematics and
 * contraptions putting blocks back turned, and dye recolours them. Positions are relative to the test's structure;
 * {@code empty} is a 5x5x5 air template in data/createdecals/structure.
 */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DecalGameTests {
    private static final String EMPTY = "empty";
    private static final DecalState R = new DecalState(Decals.id("letter_r"), DyeColor.RED.getTextureDiffuseColor(), 1, true);
    private static final DecalState SEVEN = new DecalState(Decals.id("digit_7"), DyeColor.BLUE.getTextureDiffuseColor(), 0, false, 1, 2, 2, 3);

    private DecalGameTests() {
    }

    @GameTest(template = EMPTY)
    public static void createIsLoaded(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("create"), "Create is not loaded");
        helper.assertTrue(Decals.builtIn().size() >= 27, "built-in decals not found: " + Decals.builtIn());
        helper.succeed();
    }

    /**
     * A Decal Block hides the face of the block under it once it copies a solid material, but not while it shows
     * Create's see-through copycat frame (the ground showed the sky through a bare one).
     */
    @GameTest(template = EMPTY)
    public static void bareDecalBlocksHideNothing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        BlockPos absolute = helper.absolutePos(pos);
        java.util.function.BooleanSupplier hidesStone = () -> helper.getLevel().getBlockState(absolute)
                .hidesNeighborFace(helper.getLevel(), absolute, Blocks.STONE.defaultBlockState(), Direction.DOWN);
        helper.assertFalse(hidesStone.getAsBoolean(), "a bare Decal Block hides the stone's top under it");
        DecalCopycatBlockEntity block = helper.getBlockEntity(pos);
        block.setMaterial(Blocks.WHITE_CONCRETE.defaultBlockState());
        helper.assertTrue(hidesStone.getAsBoolean(), "a white concrete Decal Block does not hide the stone's top under it");
        block.setMaterial(Blocks.GLASS.defaultBlockState());
        helper.assertFalse(hidesStone.getAsBoolean(), "a glass Decal Block hides the stone's top under it");
        helper.succeed();
    }

    /** A panel's decal and a block's decals survive saving and loading the block entity, tiles and turns included. */
    @GameTest(template = EMPTY)
    public static void decalsSurviveSaveAndLoad(GameTestHelper helper) {
        BlockPos blockPos = new BlockPos(3, 1, 2);
        helper.setBlock(blockPos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity block = helper.getBlockEntity(blockPos);
        block.setMaterial(Blocks.OAK_PLANKS.defaultBlockState());
        block.setConsumedItem(new ItemStack(Items.OAK_PLANKS));
        block.setDecal(Direction.NORTH, R);
        block.setDecal(Direction.UP, SEVEN);

        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        for (DecalCopycatBlockEntity be : new DecalCopycatBlockEntity[] {block}) {
            CompoundTag saved = be.saveWithFullMetadata(registries);
            BlockEntity loaded = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), saved, registries);
            helper.assertTrue(loaded instanceof DecalCopycatBlockEntity copy && copy.getDecals().equals(be.getDecals())
                            && copy.getMaterial() == be.getMaterial(),
                    "decals or material lost in saving: " + be.getDecals() + " became " + saved);
        }
        helper.succeed();
    }

    /** Schematics save blocks with writeSafe: decals stay, as the material does. */
    @GameTest(template = EMPTY)
    public static void schematicsKeepDecals(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity block = helper.getBlockEntity(pos);
        block.setDecal(Direction.EAST, SEVEN);
        CompoundTag tag = new CompoundTag();
        block.writeSafe(tag, helper.getLevel().registryAccess());
        helper.assertTrue(tag.contains("Decals"), "writeSafe left the decals out: " + tag);
        helper.succeed();
    }

    /**
     * A contraption turned a quarter clockwise (seen from above) puts a Decal Block back: its north decal moves to the
     * east face, still upright; its top decal stays on top, its top now pointing east (rotation 1 more). Mirrored, the
     * decal flips.
     */
    @GameTest(template = EMPTY)
    public static void contraptionsTurnDecalsWithTheBlock(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity block = helper.getBlockEntity(pos);
        DecalState upright = new DecalState(Decals.id("letter_r"), 0xFFFFFF, 0, false);
        block.setDecal(Direction.NORTH, upright);
        block.setDecal(Direction.UP, upright);
        block.transform(block, new StructureTransform(BlockPos.ZERO, Direction.Axis.Y, Rotation.CLOCKWISE_90, Mirror.NONE));
        helper.assertTrue(block.getDecals().equals(Map.of(Direction.EAST, upright, Direction.UP, upright.withTurn(1, false))),
                "turned decals: " + block.getDecals());

        block.transform(block, new StructureTransform(BlockPos.ZERO, null, Rotation.NONE, Mirror.FRONT_BACK));
        helper.assertTrue(block.getDecal(Direction.WEST) != null && block.getDecal(Direction.WEST).flip(),
                "mirrored east to west and flipped: " + block.getDecals());
        helper.succeed();
    }

    /** A dye on a face with a decal recolours that decal, through the same call a player's click makes. */
    @GameTest(template = EMPTY)
    public static void dyeRecoloursTheClickedFace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity block = helper.getBlockEntity(pos);
        block.setDecal(Direction.NORTH, R);
        block.setDecal(Direction.SOUTH, R);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack dye = new ItemStack(Items.LIME_DYE);
        player.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.useBlock(pos, player, new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(pos)).relative(Direction.NORTH, 0.5), Direction.NORTH,
                helper.absolutePos(pos), false));
        helper.assertTrue(block.getDecal(Direction.NORTH).rgb() == (DyeColor.LIME.getTextureDiffuseColor() & 0xFFFFFF),
                "north decal not recoloured: " + block.getDecal(Direction.NORTH));
        helper.assertTrue(block.getDecal(Direction.SOUTH).equals(R), "south decal changed: " + block.getDecal(Direction.SOUTH));
        helper.assertTrue(dye.getCount() == 1, "the dye was used up");
        helper.succeed();
    }
}
