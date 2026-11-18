package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create's Schematicannon: a schematic stores each block entity's {@code writeSafe} data, and the cannon places the
 * block with Create's {@code BlockHelper.placeSchematicBlock}. The Decal Block must come out with its material and every
 * decal (Create's own copycats: {@code CreateCopycatGameTests}).
 */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SchematicGameTests {
    private SchematicGameTests() {
    }

    @GameTest(template = "empty")
    public static void schematicannonPlacesDecalBlocks(GameTestHelper helper) {
        DecalState big = new DecalState(Decals.id("cog_2x2"), 0xF9801D, 1, true, 1, 0, 2, 2);
        DecalState digit = new DecalState(Decals.id("digit_4"), 0x3C44AA, 0, false);
        check(helper, DecalsBlocks.DECAL_BLOCK.getDefaultState(), new BlockPos(1, 1, 1), new BlockPos(1, 1, 3),
                Map.of(Direction.NORTH, big, Direction.UP, digit));
        helper.succeed();
    }

    private static void check(GameTestHelper helper, BlockState state, BlockPos source, BlockPos target, Map<Direction, DecalState> decals) {
        helper.setBlock(source, state);
        DecalCopycatBlockEntity original = helper.getBlockEntity(source);
        original.setMaterial(Blocks.OAK_PLANKS.defaultBlockState());
        original.setConsumedItem(new ItemStack(Items.OAK_PLANKS));
        decals.forEach(original::setDecal);

        // What a schematic keeps of the block entity, then what the cannon does with it
        CompoundTag data = new CompoundTag();
        original.writeSafe(data, helper.getLevel().registryAccess());
        BlockHelper.placeSchematicBlock(helper.getLevel(), state, helper.absolutePos(target), new ItemStack(state.getBlock()), data);

        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(target)) instanceof DecalCopycatBlockEntity placed
                        && placed.getDecals().equals(decals) && placed.getMaterial().is(Blocks.OAK_PLANKS),
                "the cannon placed " + helper.getLevel().getBlockEntity(helper.absolutePos(target)) + " from " + data);
    }
}
