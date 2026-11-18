package com.goldskinmc.createdecals.ponder;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.AttachedDecals;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalPlacement;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.registry.DecalsItems;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import java.util.Optional;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
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
import net.neoforged.neoforge.attachment.AttachmentHolder;
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
        // Create's own Copycat Panel and Step take decals as they are: their scenes sit under Create's items
        scenes.forComponents(AllBlocks.COPYCAT_PANEL).addStoryBoard("copycat_panel", DecalsScenes::copycatPanel, DECALS);
        scenes.forComponents(DecalsBlocks.DECAL_BLOCK).addStoryBoard("decal_block", DecalsScenes::decalBlock, DECALS);
        scenes.forComponents(AllBlocks.COPYCAT_STEP).addStoryBoard("copycat_step", DecalsScenes::copycatStep, DECALS);
        scenes.forComponents(DecalsItems.DECAL_STENCIL).addStoryBoard("decal_stencil", DecalsScenes::stencil, DECALS);
        // Also under Create's Deployer, so players looking up what a Deployer can do find it there
        scenes.forComponents(DecalsItems.DECAL_STENCIL, AllBlocks.DEPLOYER)
                .addStoryBoard("decal_stencil_deployer", DecalsScenes::deployer, DECALS);
    }

    static void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(DECALS)
                .addToIndex()
                .item(DecalsItems.DECAL_STENCIL.get(), true, false)
                .title("Decals")
                .description("Liveries for anything Create builds: decals on copycat blocks")
                .register();
        PonderTagRegistrationHelper<ItemProviderEntry<?, ?>> tags = helper.withKeyFunction(RegistryEntry::getId);
        tags.addToTag(DECALS)
                .add(DecalsItems.DECAL_STENCIL)
                .add(DecalsBlocks.DECAL_BLOCK)
                .add(AllBlocks.COPYCAT_PANEL)
                .add(AllBlocks.COPYCAT_STEP);
    }

    static void copycatPanel(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("copycat_panel", "Putting a decal on a Copycat Panel");
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
                .text("Copycat Panels take decals")
                .placeNearTarget().pointAt(front);
        scene.idle(80);
        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(new ItemStack(Items.WHITE_CONCRETE));
        scene.idle(7);
        scene.world().modifyBlockEntityNBT(util.select().position(panel), CopycatBlockEntity.class, nbt -> {
            nbt.put("Material", NbtUtils.writeBlockState(Blocks.WHITE_CONCRETE.defaultBlockState()));
            CompoundTag item = new CompoundTag();
            item.putString("id", "minecraft:white_concrete");
            item.putInt("count", 1);
            nbt.put("Item", item);
        }, true);
        scene.idle(30);

        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(stencil("digit_7", DyeColor.RED, 0, false, 1, 1));
        scene.idle(7);
        attached(scene, util, panel, Direction.NORTH, new DecalState(Decals.id("digit_7"), rgb(DyeColor.RED), 0, false));
        scene.overlay().showText(70).attachKeyFrame()
                .text("Right-click with a Decal Stencil to put one on")
                .placeNearTarget().pointAt(front);
        scene.idle(80);

        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(new ItemStack(Items.BLUE_DYE));
        scene.idle(7);
        attached(scene, util, panel, Direction.NORTH, new DecalState(Decals.id("digit_7"), rgb(DyeColor.BLUE), 0, false));
        scene.overlay().showText(60).text("Dye recolours it").placeNearTarget().pointAt(front);
        scene.idle(70);
        scene.overlay().showText(90).attachKeyFrame()
                .text("Without Create: Decals, it is a plain Copycat Panel again")
                .placeNearTarget().pointAt(front);
        scene.idle(100);
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
                .text("A full copycat block, for hulls of whole blocks")
                .placeNearTarget().pointAt(top);
        scene.idle(80);
        scene.overlay().showControls(north, Pointing.DOWN, 30).rightClick().withItem(stencil("letter_a", DyeColor.BLACK, 0, false, 1, 1));
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
                .text("Each face takes its own decal")
                .placeNearTarget().pointAt(north);
        scene.idle(80);

        ItemStack frontStencil = stencil("frame", DyeColor.RED, 0, false, 1, 1);
        frontStencil.set(DecalsComponents.STENCIL.get(), DecalStencilItem.data(frontStencil).withFront(true));
        scene.overlay().showControls(north, Pointing.DOWN, 30).rightClick().withItem(frontStencil);
        scene.idle(7);
        DecalState frame = new DecalState(Decals.id("frame"), rgb(DyeColor.RED), 0, false);
        decal(scene, util, block, Direction.NORTH, true, frame);
        scene.overlay().showText(80).attachKeyFrame()
                .text("Set the stencil to the front layer to put a second decal on top")
                .placeNearTarget().pointAt(north);
        scene.idle(90);
        scene.overlay().showControls(north, Pointing.DOWN, 30).rightClick().withItem(new ItemStack(Items.GLOW_INK_SAC));
        scene.idle(7);
        decal(scene, util, block, Direction.NORTH, true, frame.withGlow(true));
        scene.overlay().showText(70)
                .text("A Glow Ink Sac makes a decal glow; an Ink Sac stops it")
                .placeNearTarget().pointAt(north);
        scene.idle(80);
    }

    static void copycatStep(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("copycat_step", "Decals on Copycat Steps");
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
                .text("Copycat Steps take a decal on any face")
                .placeNearTarget().pointAt(stepFace);
        scene.idle(80);
        scene.overlay().showControls(blockFace, Pointing.DOWN, 30).rightClick().withItem(stencil("hazard", DyeColor.YELLOW, 0, false, 1, 1));
        scene.idle(7);
        decal(scene, util, Layouts.Step.BLOCK, Direction.NORTH, new DecalState(Decals.id("hazard"), rgb(DyeColor.YELLOW), 0, false));
        scene.idle(20);
        scene.overlay().showControls(stepFace, Pointing.DOWN, 30).rightClick().withItem(stencil("hazard", DyeColor.YELLOW, 0, false, 1, 1));
        scene.idle(7);
        attached(scene, util, Layouts.Step.STEP, Direction.NORTH, new DecalState(Decals.id("hazard"), rgb(DyeColor.YELLOW), 0, false));
        scene.overlay().showText(90).attachKeyFrame()
                .text("Each face shows its part of the decal, so stripes run on from the blocks beside it")
                .placeNearTarget().pointAt(stepFace);
        scene.idle(100);
        scene.overlay().showControls(stepTop, Pointing.DOWN, 30).rightClick().withItem(stencil("stripe_double", DyeColor.BLACK, 1, false, 1, 1));
        scene.idle(7);
        attached(scene, util, Layouts.Step.STEP, Direction.UP, new DecalState(Decals.id("stripe_double"), rgb(DyeColor.BLACK), 1, false));
        scene.idle(40);
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
                .text("Use it in the air to pick a decal, a word or a colour")
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
                .text("Big decals spread right and down from the face you click")
                .placeNearTarget().pointAt(topLeftFace);
        scene.idle(90);

        scene.overlay().showControls(middleFace, Pointing.DOWN, 40).rightClick().whileSneaking()
                .withItem(new ItemStack(DecalsItems.DECAL_STENCIL.get()));
        scene.idle(20);
        scene.overlay().showControls(sideFace, Pointing.DOWN, 40).rightClick().withItem(stencil("cog", DyeColor.ORANGE, 0, false, 1, 1));
        scene.idle(7);
        decal(scene, util, Layouts.Stencil.SIDE, Direction.NORTH, new DecalState(Decals.id("cog"), rgb(DyeColor.ORANGE), 0, false));
        scene.overlay().showText(80).attachKeyFrame()
                .text("Sneak-click a decal to copy it into the stencil")
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
                .text("The eraser takes a decal off. Create's Clipboard copies them")
                .placeNearTarget().pointAt(middleFace);
        scene.idle(90);
    }

    static void deployer(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("decal_stencil_deployer", "Stamping decals with Deployers");
        scene.configureBasePlate(0, 0, Layouts.Deployer.SIZE);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos deployerPos = Layouts.Deployer.DEPLOYER;
        BlockPos middle = Layouts.Deployer.MIDDLE;
        Selection shaft = util.select().fromTo(Layouts.Deployer.SHAFT_FROM, Layouts.Deployer.SHAFT_TO);
        Selection kinetics = shaft.copy().add(util.select().position(deployerPos));
        scene.world().showSection(util.select().position(middle), Direction.DOWN);
        scene.idle(5);
        ElementLink<WorldSectionElement> deployer = scene.world().showIndependentSection(util.select().position(deployerPos), Direction.DOWN);
        scene.world().showSection(shaft, Direction.DOWN);
        scene.idle(15);
        Vec3 top = util.vector().blockSurface(middle, Direction.UP);

        held(scene, util, deployerPos, stencil("cog", DyeColor.ORANGE, 0, false, 1, 1));
        scene.world().setKineticSpeed(kinetics, 32);
        scene.overlay().showText(80).attachKeyFrame()
                .text("A Deployer with a stencil stamps the block in front of it")
                .placeNearTarget().pointAt(util.vector().topOf(deployerPos));
        scene.idle(30);
        stamp(scene, util, deployerPos, middle, new DecalState(Decals.id("cog"), rgb(DyeColor.ORANGE), 0, false));
        scene.idle(40);

        held(scene, util, deployerPos, new ItemStack(Items.BLUE_DYE));
        scene.overlay().showText(70).attachKeyFrame()
                .text("With dye it recolours; with the eraser it erases")
                .placeNearTarget().pointAt(top);
        scene.idle(20);
        stamp(scene, util, deployerPos, middle, new DecalState(Decals.id("cog"), rgb(DyeColor.BLUE), 0, false));
        ItemStack eraser = new ItemStack(DecalsItems.DECAL_STENCIL.get());
        eraser.set(DecalsComponents.STENCIL.get(), StencilData.EMPTY.withErase(true));
        held(scene, util, deployerPos, eraser);
        scene.idle(10);
        stamp(scene, util, deployerPos, middle, null);
        scene.idle(30);

        scene.world().setKineticSpeed(kinetics, 0);
        scene.world().hideSection(shaft, Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().position(Layouts.Deployer.ROW_FROM), Direction.DOWN);
        scene.world().showSection(util.select().position(Layouts.Deployer.ROW_TO), Direction.DOWN);
        scene.idle(15);
        held(scene, util, deployerPos, stencil("stripe", DyeColor.RED, 0, false, 1, 1));
        scene.world().moveSection(deployer, new Vec3(1, 0, 0), 15);
        scene.idle(15);
        scene.overlay().showText(90).attachKeyFrame()
                .text("On a contraption, it stamps every block it passes")
                .placeNearTarget().pointAt(util.vector().topOf(middle.east()));
        DecalState stripe = new DecalState(Decals.id("stripe"), rgb(DyeColor.RED), 0, false);
        for (int x = Layouts.Deployer.ROW_TO.getX(); x >= Layouts.Deployer.ROW_FROM.getX(); x--) {
            // A moving Deployer reaches out at each block it works on, as Create's does on a contraption
            scene.world().moveDeployer(deployerPos, 1, 9);
            scene.idle(10);
            decal(scene, util, new BlockPos(x, 1, 2), Direction.UP, stripe);
            scene.world().moveDeployer(deployerPos, -1, 9);
            scene.idle(10);
            if (x > Layouts.Deployer.ROW_FROM.getX()) {
                scene.world().moveSection(deployer, new Vec3(-1, 0, 0), 15);
                scene.idle(15);
            }
        }
        scene.idle(60);
    }

    /** Puts {@code stack} in the Deployer's hand, through its saved data as Create's own scenes do. */
    private static void held(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos deployer, ItemStack stack) {
        scene.world().modifyBlockEntityNBT(util.select().position(deployer), DeployerBlockEntity.class,
                nbt -> nbt.put("HeldItem", stack.saveOptional(scene.world().getHolderLookupProvider())));
    }

    /** The Deployer reaches out, the face below changes (null: the decal goes), and it pulls back. */
    private static void stamp(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos deployer, BlockPos target,
            @Nullable DecalState decal) {
        scene.world().moveDeployer(deployer, 1, 25);
        scene.idle(26);
        decal(scene, util, target, Direction.UP, decal);
        scene.world().moveDeployer(deployer, -1, 25);
        scene.idle(25);
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

    /**
     * Sets (or with null removes) the decal on one face of Create's copycat: kept in our attachment on its block entity,
     * written where NeoForge saves attachments in the block entity's data, which Ponder loads back and redraws.
     */
    private static void attached(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, Direction face, @Nullable DecalState decal) {
        scene.world().modifyBlockEntityNBT(util.select().position(pos), CopycatBlockEntity.class, nbt -> {
            CompoundTag attachments = nbt.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY);
            String key = CreateDecals.id("decals").toString();
            AttachedDecals decals = attachments.contains(key) ? AttachedDecals.load(attachments.getCompound(key)) : AttachedDecals.EMPTY;
            attachments.put(key, decals.with(face, false, decal).save());
            nbt.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, attachments);
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
