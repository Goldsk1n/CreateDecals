package com.goldskinmc.createdecals.ponder;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalPlacement;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.registry.DecalsItems;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import java.util.Optional;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Ponder scenes: the Decal Panel, the Decal Block, and the Decal Stencil with large decals. They are built on the
 * schematics from {@link PonderSchematics}; each click in a scene sets what the real click would on the block entity
 * (through its saved data, so Ponder redraws the block).
 */
final class DecalsScenes {
    static final ResourceLocation DECALS = CreateDecals.id("decals");

    private DecalsScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> scenes = helper.withKeyFunction(RegistryEntry::getId);
        scenes.forComponents(DecalsBlocks.DECAL_PANEL).addStoryBoard("decal_panel", DecalsScenes::decalPanel, DECALS);
        scenes.forComponents(DecalsBlocks.DECAL_BLOCK).addStoryBoard("decal_block", DecalsScenes::decalBlock, DECALS);
        scenes.forComponents(DecalsBlocks.DECAL_STEP).addStoryBoard("decal_step", DecalsScenes::decalStep, DECALS);
        scenes.forComponents(DecalsItems.DECAL_STENCIL).addStoryBoard("decal_stencil", DecalsScenes::stencil, DECALS);
    }

    static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(DECALS)
                .addToIndex()
                .item(DecalsBlocks.DECAL_PANEL.get(), true, false)
                .title("Decals")
                .description("Liveries for anything Create builds: decals on copycat blocks")
                .register();
        PonderTagRegistrationHelper<ItemProviderEntry<?, ?>> tags = helper.withKeyFunction(RegistryEntry::getId);
        tags.addToTag(DECALS)
                .add(DecalsItems.DECAL_STENCIL)
                .add(DecalsBlocks.DECAL_PANEL)
                .add(DecalsBlocks.DECAL_BLOCK)
                .add(DecalsBlocks.DECAL_STEP);
    }

    static void decalPanel(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("decal_panel", "Putting a decal on a Decal Panel");
        scene.configureBasePlate(0, 0, Layouts.Panel.SIZE);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(Layouts.Panel.WALL_FROM, Layouts.Panel.WALL_TO), Direction.DOWN);
        scene.idle(10);
        BlockPos panel = Layouts.Panel.PANEL;
        scene.world().showSection(util.select().position(panel), Direction.SOUTH);
        scene.idle(20);
        Vec3 front = util.vector().blockSurface(panel, Direction.NORTH).add(0, 0, 0.3);

        scene.overlay().showText(70).attachKeyFrame()
                .text("A Decal Panel is a Copycat Panel that can show a decal on its front")
                .placeNearTarget().pointAt(front);
        scene.idle(80);
        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(new ItemStack(Items.WHITE_CONCRETE));
        scene.idle(7);
        scene.world().modifyBlockEntityNBT(util.select().position(panel), DecalCopycatBlockEntity.class, nbt -> {
            nbt.put("Material", NbtUtils.writeBlockState(Blocks.WHITE_CONCRETE.defaultBlockState()));
            CompoundTag item = new CompoundTag();
            item.putString("id", "minecraft:white_concrete");
            item.putInt("count", 1);
            nbt.put("Item", item);
        }, true);
        scene.overlay().showText(60).text("Like any copycat, it takes the look of a block used on it")
                .placeNearTarget().pointAt(front);
        scene.idle(70);

        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(stencil("digit_7", DyeColor.RED, 0, false, 1, 1));
        scene.idle(7);
        decal(scene, util, panel, Direction.NORTH, new DecalState(Decals.id("digit_7"), rgb(DyeColor.RED), 0, false));
        scene.overlay().showText(70).attachKeyFrame()
                .text("Right-click it with a Decal Stencil to put the stencil's decal on it. Create's own Copycat Panels turn into Decal Panels the same way")
                .placeNearTarget().pointAt(front);
        scene.idle(80);

        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(new ItemStack(Items.BLUE_DYE));
        scene.idle(7);
        decal(scene, util, panel, Direction.NORTH, new DecalState(Decals.id("digit_7"), rgb(DyeColor.BLUE), 0, false));
        scene.overlay().showText(60).text("A dye recolours the decal").placeNearTarget().pointAt(front);
        scene.idle(70);
        scene.overlay().showText(80).attachKeyFrame()
                .text("The decal is part of the block: it stays on trains, contraptions and Sable vehicles, and under shaders")
                .placeNearTarget().pointAt(front);
        scene.idle(90);
    }

    static void decalBlock(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("decal_block", "Decals on a Decal Block");
        scene.configureBasePlate(0, 0, Layouts.Block.SIZE);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos block = Layouts.Block.BLOCK;
        scene.world().showSection(util.select().position(block), Direction.DOWN);
        scene.idle(20);
        Vec3 north = util.vector().blockSurface(block, Direction.NORTH);
        Vec3 west = util.vector().blockSurface(block, Direction.WEST);
        Vec3 top = util.vector().topOf(block);

        scene.overlay().showText(70).attachKeyFrame()
                .text("A Decal Block is a full copycat block, for hulls built of whole blocks")
                .placeNearTarget().pointAt(top);
        scene.idle(80);
        scene.overlay().showControls(north, Pointing.LEFT, 30).rightClick().withItem(stencil("letter_a", DyeColor.BLACK, 0, false, 1, 1));
        scene.idle(7);
        decal(scene, util, block, Direction.NORTH, new DecalState(Decals.id("letter_a"), rgb(DyeColor.BLACK), 0, false));
        scene.idle(20);
        scene.overlay().showControls(west, Pointing.RIGHT, 30).rightClick().withItem(stencil("hazard", DyeColor.ORANGE, 0, false, 1, 1));
        scene.idle(7);
        decal(scene, util, block, Direction.WEST, new DecalState(Decals.id("hazard"), rgb(DyeColor.ORANGE), 0, false));
        scene.idle(20);
        scene.overlay().showControls(top, Pointing.DOWN, 30).rightClick().withItem(stencil("chevron", DyeColor.BLUE, 1, false, 1, 1));
        scene.idle(7);
        decal(scene, util, block, Direction.UP, new DecalState(Decals.id("chevron"), rgb(DyeColor.BLUE), 1, false));
        scene.overlay().showText(70).attachKeyFrame()
                .text("Each of its faces takes a decal of its own: the stencil puts it on the face you click")
                .placeNearTarget().pointAt(north);
        scene.idle(80);
        scene.overlay().showText(70)
                .text("Faces against a solid block are hidden, decal and all, as on any block")
                .placeNearTarget().pointAt(west);
        scene.idle(80);

        ItemStack frontStencil = stencil("frame", DyeColor.RED, 0, false, 1, 1);
        frontStencil.set(DecalsComponents.STENCIL.get(), DecalStencilItem.data(frontStencil).withFront(true));
        scene.overlay().showControls(north, Pointing.LEFT, 30).rightClick().withItem(frontStencil);
        scene.idle(7);
        DecalState frame = new DecalState(Decals.id("frame"), rgb(DyeColor.RED), 0, false);
        decal(scene, util, block, Direction.NORTH, true, frame);
        scene.overlay().showText(80).attachKeyFrame()
                .text("Set the stencil to the front layer to put a second decal over the first, such as a number on a stripe")
                .placeNearTarget().pointAt(north);
        scene.idle(90);
        scene.overlay().showControls(north, Pointing.LEFT, 30).rightClick().withItem(new ItemStack(Items.GLOW_INK_SAC));
        scene.idle(7);
        decal(scene, util, block, Direction.NORTH, true, frame.withGlow(true));
        scene.overlay().showText(70)
                .text("A Glow Ink Sac makes a decal glow in the dark; an Ink Sac stops it")
                .placeNearTarget().pointAt(north);
        scene.idle(80);
    }

    static void decalStep(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("decal_step", "Decals on Decal Steps");
        scene.configureBasePlate(0, 0, Layouts.Step.SIZE);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(Layouts.Step.BLOCK), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(Layouts.Step.STEP), Direction.DOWN);
        scene.idle(20);
        Vec3 blockFace = util.vector().blockSurface(Layouts.Step.BLOCK, Direction.NORTH);
        Vec3 stepFace = util.vector().blockSurface(Layouts.Step.STEP, Direction.NORTH).add(0, -0.25, 0);
        Vec3 stepTop = util.vector().centerOf(Layouts.Step.STEP).add(0, 0, -0.25);

        scene.overlay().showText(70).attachKeyFrame()
                .text("A Decal Step is a Copycat Step that takes a decal on any of its faces")
                .placeNearTarget().pointAt(stepFace);
        scene.idle(80);
        scene.overlay().showControls(blockFace, Pointing.DOWN, 30).rightClick().withItem(stencil("hazard", DyeColor.YELLOW, 0, false, 1, 1));
        scene.idle(7);
        decal(scene, util, Layouts.Step.BLOCK, Direction.NORTH, new DecalState(Decals.id("hazard"), rgb(DyeColor.YELLOW), 0, false));
        scene.idle(20);
        scene.overlay().showControls(stepFace, Pointing.DOWN, 30).rightClick().withItem(stencil("hazard", DyeColor.YELLOW, 0, false, 1, 1));
        scene.idle(7);
        decal(scene, util, Layouts.Step.STEP, Direction.NORTH, new DecalState(Decals.id("hazard"), rgb(DyeColor.YELLOW), 0, false));
        scene.overlay().showText(90).attachKeyFrame()
                .text("Each face shows the part of the decal it covers, as if the decal lay on the whole face of the block, so stripes and patterns run on from the Decal Blocks beside it")
                .placeNearTarget().pointAt(stepFace);
        scene.idle(100);
        scene.overlay().showControls(stepTop, Pointing.DOWN, 30).rightClick().withItem(stencil("stripe_double", DyeColor.BLACK, 1, false, 1, 1));
        scene.idle(7);
        decal(scene, util, Layouts.Step.STEP, Direction.UP, new DecalState(Decals.id("stripe_double"), rgb(DyeColor.BLACK), 1, false));
        scene.overlay().showText(70)
                .text("Letters and numbers read best on Decal Blocks and Panels, where the whole decal shows")
                .placeNearTarget().pointAt(stepTop);
        scene.idle(80);
        scene.overlay().showText(80).attachKeyFrame()
                .text("Create's own Copycat Steps turn into Decal Steps when a stencil puts a decal on them")
                .placeNearTarget().pointAt(stepFace);
        scene.idle(90);
    }

    static void stencil(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("decal_stencil", "Choosing, placing and picking up decals");
        scene.configureBasePlate(0, 0, Layouts.Stencil.SIZE);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(Layouts.Stencil.WALL_FROM, Layouts.Stencil.WALL_TO), Direction.DOWN);
        scene.world().showSection(util.select().position(Layouts.Stencil.SIDE), Direction.DOWN);
        scene.idle(20);
        BlockPos topLeft = Layouts.Stencil.TOP_LEFT;
        Vec3 topLeftFace = util.vector().blockSurface(topLeft, Direction.NORTH);
        Vec3 middleFace = util.vector().blockSurface(Layouts.Stencil.MIDDLE, Direction.NORTH);
        Vec3 sideFace = util.vector().blockSurface(Layouts.Stencil.SIDE, Direction.NORTH);

        scene.overlay().showText(80).attachKeyFrame()
                .text("Use the Decal Stencil in the air to choose a decal or type a word, its colour, and how it is turned and flipped")
                .placeNearTarget().pointAt(middleFace);
        scene.idle(90);

        DecalState cog = new DecalState(Decals.id("cog_3x3"), rgb(DyeColor.ORANGE), 0, false, 0, 0, 3, 3);
        scene.overlay().showControls(topLeftFace, Pointing.DOWN, 40).rightClick().withItem(stencil("cog_3x3", DyeColor.ORANGE, 0, false, 3, 3));
        scene.overlay().showOutline(PonderPalette.GREEN, "wall", util.select().fromTo(Layouts.Stencil.WALL_FROM, Layouts.Stencil.WALL_TO), 40);
        scene.idle(7);
        for (DecalPlacement.Tile tile : DecalPlacement.tiles(topLeft, Direction.NORTH, cog)) {
            decal(scene, util, tile.pos(), Direction.NORTH, tile.decal());
        }
        scene.overlay().showText(80).attachKeyFrame()
                .text("Decals larger than one block spread right and down from the face you click, over the decal blocks beside it")
                .placeNearTarget().pointAt(topLeftFace);
        scene.idle(90);

        scene.overlay().showControls(middleFace, Pointing.DOWN, 40).rightClick().whileSneaking()
                .withItem(new ItemStack(DecalsItems.DECAL_STENCIL.get()));
        scene.idle(20);
        scene.overlay().showControls(sideFace, Pointing.DOWN, 40).rightClick().withItem(stencil("cog", DyeColor.ORANGE, 0, false, 1, 1));
        scene.idle(7);
        decal(scene, util, Layouts.Stencil.SIDE, Direction.NORTH, new DecalState(Decals.id("cog"), rgb(DyeColor.ORANGE), 0, false));
        scene.overlay().showText(80).attachKeyFrame()
                .text("Sneak and right-click a decal to pick it up into the stencil, then use it elsewhere")
                .placeNearTarget().pointAt(sideFace);
        scene.idle(90);

        ItemStack eraser = new ItemStack(DecalsItems.DECAL_STENCIL.get());
        eraser.set(DecalsComponents.STENCIL.get(), StencilData.EMPTY.withErase(true));
        scene.overlay().showControls(middleFace, Pointing.DOWN, 40).rightClick().withItem(eraser);
        scene.idle(7);
        for (DecalPlacement.Tile tile : DecalPlacement.tiles(topLeft, Direction.NORTH, cog)) {
            decal(scene, util, tile.pos(), Direction.NORTH, null);
        }
        scene.overlay().showText(80).attachKeyFrame()
                .text("The stencil's eraser takes a whole decal off, clicked on any of its tiles. Create's Clipboard copies decals too")
                .placeNearTarget().pointAt(middleFace);
        scene.idle(90);
    }

    /**
     * Sets (or with null removes) the decal on one face, through the block entity's saved data: Ponder redraws a scene's
     * blocks only after an NBT change, not after a plain block-entity call.
     */
    private static void decal(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Direction face, @Nullable DecalState decal) {
        decal(scene, util, pos, face, false, decal);
    }

    /** {@link #decal(CreateSceneBuilder, SceneBuildingUtil, BlockPos, Direction, DecalState)} in the front or back layer. */
    private static void decal(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Direction face, boolean front,
            @Nullable DecalState decal) {
        String key = front ? "FrontDecals" : "Decals";
        scene.world().modifyBlockEntityNBT(util.select().position(pos), DecalCopycatBlockEntity.class, nbt -> {
            CompoundTag faces = nbt.getCompound(key);
            if (decal == null) {
                faces.remove(face.getName());
            } else {
                faces.put(face.getName(), decal.save());
            }
            nbt.put(key, faces);
        }, true);
    }

    private static ItemStack stencil(String decal, DyeColor dye, int rotation, boolean flip, int width, int height) {
        ItemStack stack = new ItemStack(DecalsItems.DECAL_STENCIL.get());
        stack.set(DecalsComponents.STENCIL.get(), new StencilData(Optional.of(Decals.id(decal)), rgb(dye), rotation, flip, false, width, height));
        return stack;
    }

    private static int rgb(DyeColor dye) {
        return dye.getTextureDiffuseColor() & 0xFFFFFF;
    }
}
