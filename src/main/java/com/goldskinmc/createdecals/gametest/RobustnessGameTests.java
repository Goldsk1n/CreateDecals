package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** What schematics ask of decal blocks, and decal data that is damaged or hand-edited. */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobustnessGameTests {
    private static final DecalState R = new DecalState(Decals.id("letter_r"), 0xFFFFFF, 0, false);

    private RobustnessGameTests() {
    }

    /**
     * The Schematicannon turns and mirrors every block entity of a schematic placed turned or mirrored through
     * {@code StructureTransform.apply} before printing it (Create's {@code SchematicPrinter}). Mirrored left to right
     * (north and south swap) and turned a quarter clockwise, a north decal ends on the west face, flipped, and the top
     * decal stays on top, flipped: a mirrored letter reads backwards, as a mirrored build is.
     */
    @GameTest(template = "empty")
    public static void schematicPrinterTurnsAndMirrorsDecals(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity block = helper.getBlockEntity(pos);
        block.setDecal(Direction.NORTH, R);
        block.setDecal(Direction.UP, R);
        new StructureTransform(BlockPos.ZERO, Direction.Axis.Y, Rotation.CLOCKWISE_90, Mirror.LEFT_RIGHT).apply(block);
        DecalState west = block.getDecal(Direction.WEST);
        DecalState up = block.getDecal(Direction.UP);
        helper.assertTrue(block.getDecals().size() == 2 && west != null && west.id().equals(R.id()) && west.flip(),
                "north decal after mirroring and turning: " + block.getDecals());
        helper.assertTrue(up != null && up.flip(), "top decal after mirroring and turning: " + block.getDecals());
        helper.succeed();
    }

    /** A Schematicannon printing a Decal Block asks for the block and for the material it copies, as for Create's copycats. */
    @GameTest(template = "empty")
    public static void schematicannonAsksForTheMaterial(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity block = helper.getBlockEntity(pos);
        block.setMaterial(Blocks.WHITE_CONCRETE.defaultBlockState());
        block.setConsumedItem(new ItemStack(Items.WHITE_CONCRETE));
        block.setDecal(Direction.NORTH, R);
        List<Item> items = ItemRequirement.of(block.getBlockState(), block).getRequiredItems().stream()
                .map(required -> required.stack.getItem()).toList();
        helper.assertTrue(items.contains(DecalsBlocks.DECAL_BLOCK.asItem()) && items.contains(Items.WHITE_CONCRETE),
                "the Schematicannon asks for " + items);
        helper.succeed();
    }

    /**
     * Damaged or hand-edited decal data loads without an error: a face whose id is not an id shows no decal (not another
     * one), a face name that is not a direction is ignored, and numbers out of range are brought into range.
     */
    @GameTest(template = "empty")
    public static void damagedDecalDataLoads(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, DecalsBlocks.DECAL_BLOCK.getDefaultState());
        DecalCopycatBlockEntity block = helper.getBlockEntity(pos);
        block.setDecal(Direction.NORTH, R);
        block.setDecal(Direction.UP, R);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag saved = block.saveWithFullMetadata(registries);
        CompoundTag faces = saved.getCompound("Decals");
        faces.getCompound("north").putString("Id", "Not An Id!!");
        CompoundTag up = faces.getCompound("up");
        up.putByte("Rot", (byte) 99);
        up.putByte("TileX", (byte) 50);
        up.putByte("W", (byte) 0);
        up.putByte("H", (byte) 9);
        up.putInt("Rgb", -1);
        faces.put("sideways", R.save());
        BlockEntity loaded = BlockEntity.loadStatic(block.getBlockPos(), block.getBlockState(), saved, registries);
        helper.assertTrue(loaded instanceof DecalCopycatBlockEntity, "damaged data did not load: " + saved);
        DecalCopycatBlockEntity copy = (DecalCopycatBlockEntity) loaded;
        helper.assertTrue(copy.getDecal(Direction.NORTH) == null, "a face whose id is not an id shows " + copy.getDecal(Direction.NORTH));
        DecalState top = copy.getDecal(Direction.UP);
        helper.assertTrue(top != null && top.rotation() == 3 && top.width() == 1 && top.height() == 4 && top.tileX() == 0
                && top.rgb() == 0xFFFFFF, "out-of-range numbers on top: " + top);
        helper.assertTrue(copy.getDecals().size() == 1, "decals after loading damaged data: " + copy.getDecals());
        helper.succeed();
    }
}
