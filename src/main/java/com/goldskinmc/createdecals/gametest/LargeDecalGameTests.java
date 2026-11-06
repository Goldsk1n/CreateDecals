package com.goldskinmc.createdecals.gametest;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.registry.DecalsItems;
import com.goldskinmc.createdecals.stencil.StencilData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Decals larger than one block, through a player's clicks. The wall is 3x3 Decal Blocks at x 1-3, y 1-3, z 2, decals on
 * their north faces. Seen from the north, right is west, so the top-left face is x 3, y 3 and the top right x 1, y 3.
 */
@GameTestHolder(CreateDecals.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LargeDecalGameTests {
    private static final Direction FACE = Direction.NORTH;
    private static final BlockPos TOP_LEFT = new BlockPos(3, 3, 2);
    private static final BlockPos TOP_RIGHT = new BlockPos(1, 3, 2);
    private static final BlockPos BOTTOM_RIGHT = new BlockPos(1, 1, 2);
    private static final BlockPos MIDDLE = new BlockPos(2, 2, 2);

    private LargeDecalGameTests() {
    }

    private static void wall(GameTestHelper helper) {
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.setBlock(new BlockPos(x, y, 2), DecalsBlocks.DECAL_BLOCK.getDefaultState());
            }
        }
    }

    private static Player playerWith(GameTestHelper helper, int rotation, boolean flip, boolean erase) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(DecalsItems.DECAL_STENCIL.get());
        stack.set(DecalsComponents.STENCIL.get(), new StencilData(Optional.of(Decals.id("cog_3x3")), 0xFFFFFF, rotation, flip, erase, 3, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static void click(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        helper.useBlock(pos, player, new BlockHitResult(Vec3.atCenterOf(absolute).relative(FACE, 0.5), FACE, absolute, false));
    }

    private static DecalState decalAt(GameTestHelper helper, BlockPos pos) {
        return ((DecalCopycatBlockEntity) helper.getBlockEntity(pos)).getDecal(FACE);
    }

    /** One click lays all nine tiles: unturned, tile (0,0) on the clicked face and (2,2) bottom right. */
    @GameTest(template = "empty")
    public static void threeByThreeLandsOnNineFaces(GameTestHelper helper) {
        wall(helper);
        click(helper, playerWith(helper, 0, false, false), TOP_LEFT);
        int tiles = 0;
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                DecalState decal = decalAt(helper, new BlockPos(x, y, 2));
                helper.assertTrue(decal != null && decal.width() == 3 && decal.height() == 3, "no 3x3 tile at " + x + "," + y);
                // x 3 is the left column (tile 0), y 3 the top row (tile 0)
                helper.assertTrue(decal.tileX() == 3 - x && decal.tileY() == 3 - y, "wrong tile at " + x + "," + y + ": " + decal);
                tiles++;
            }
        }
        helper.assertTrue(tiles == 9, "tiles: " + tiles);
        helper.succeed();
    }

    /** Turned a quarter clockwise, the picture's bottom-left tile shows top left and its top-left tile top right. */
    @GameTest(template = "empty")
    public static void turnedDecalMovesItsTiles(GameTestHelper helper) {
        wall(helper);
        click(helper, playerWith(helper, 1, false, false), TOP_LEFT);
        DecalState topLeft = decalAt(helper, TOP_LEFT);
        DecalState topRight = decalAt(helper, TOP_RIGHT);
        helper.assertTrue(topLeft.tileX() == 0 && topLeft.tileY() == 2 && topLeft.rotation() == 1, "top left shows " + topLeft);
        helper.assertTrue(topRight.tileX() == 0 && topRight.tileY() == 0, "top right shows " + topRight);
        helper.succeed();
    }

    /** A face missing from the footprint is skipped; the other tiles still land. */
    @GameTest(template = "empty")
    public static void missingFaceIsSkipped(GameTestHelper helper) {
        wall(helper);
        helper.setBlock(MIDDLE, Blocks.STONE);
        click(helper, playerWith(helper, 0, false, false), TOP_LEFT);
        int tiles = 0;
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                if (helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(x, y, 2))) instanceof DecalCopycatBlockEntity be
                        && be.getDecal(FACE) != null) {
                    tiles++;
                }
            }
        }
        helper.assertTrue(tiles == 8, "tiles placed round a stone: " + tiles);
        helper.succeed();
    }

    /** The eraser on any tile takes the whole decal off; dye on any tile recolours all of it. */
    @GameTest(template = "empty")
    public static void eraserAndDyeTakeTheWholeDecal(GameTestHelper helper) {
        wall(helper);
        click(helper, playerWith(helper, 2, true, false), TOP_LEFT);
        Player dyer = helper.makeMockPlayer(GameType.SURVIVAL);
        dyer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CYAN_DYE));
        click(helper, dyer, BOTTOM_RIGHT);
        int cyan = DyeColor.CYAN.getTextureDiffuseColor() & 0xFFFFFF;
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.assertTrue(decalAt(helper, new BlockPos(x, y, 2)).rgb() == cyan, "not recoloured at " + x + "," + y);
            }
        }
        click(helper, playerWith(helper, 0, false, true), MIDDLE);
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                helper.assertTrue(decalAt(helper, new BlockPos(x, y, 2)) == null, "left by the eraser at " + x + "," + y);
            }
        }
        helper.succeed();
    }
}
