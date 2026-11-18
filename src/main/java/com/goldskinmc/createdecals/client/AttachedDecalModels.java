package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.AttachedCopycats;
import net.minecraft.core.registries.BuiltInRegistries;
import com.goldskinmc.createdecals.compat.CopycatsPlus;
import com.goldskinmc.createdecals.compat.copycats.CopycatsPlusClient;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Puts {@link AttachedDecalModel} round each attached copycat's model (Create's Copycat Panel and Step, Copycats+'s
 * copycats: {@link AttachedCopycats}) and gives them a colour handler that knows decal tints, which replaces theirs and
 * still calls it.
 * <p>
 * Ours must wrap the copycat's finished model: Create's copycat model draws its material and never asks a model inside
 * it, so decals inside it are lost. Both Create and Copycats+ put theirs on through Create's model swapper, which applies
 * each block's functions in the order they were registered; ours is registered there in the block registry event, which
 * reaches mods in load order, after Create and Copycats+. Wrapping in the model bake event alone depended on the order
 * mods hear it, and ModernFix's dynamic resources (on in All the Mods 10) send it to one mod after another in no set
 * order: Create's model then went on outside ours and no decal showed. The bake event still wraps copycats registered
 * after us.
 */
@EventBusSubscriber(modid = CreateDecals.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AttachedDecalModels {
    /** Copycats whose model Create's model swapper wraps for us. */
    private static final Set<Block> THROUGH_CREATE = Collections.newSetFromMap(new IdentityHashMap<>());

    private AttachedDecalModels() {
    }

    private static List<Block> copycats() {
        return AttachedCopycats.blocks();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerWithCreate(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK)) {
            return;
        }
        for (Block block : copycats()) {
            if (THROUGH_CREATE.add(block)) {
                CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(BuiltInRegistries.BLOCK.getKey(block), AttachedDecalModel::new);
            }
        }
        CreateDecals.LOGGER.info("Decals: on {} copycats' models through Create's model swapper", THROUGH_CREATE.size());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        int wrapped = 0;
        String example = null;
        for (Block block : copycats()) {
            if (THROUGH_CREATE.contains(block)) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = event.getModels().get(location);
                if (model != null && !(model instanceof AttachedDecalModel)) {
                    if (example == null) {
                        example = BuiltInRegistries.BLOCK.getKey(block) + " (" + model.getClass().getSimpleName() + ")";
                    }
                    event.getModels().put(location, new AttachedDecalModel(model));
                    wrapped++;
                }
            }
        }
        if (wrapped > 0) {
            CreateDecals.LOGGER.info("Decals: on {} copycat block states' models, such as {}", wrapped, example);
        }
    }

    /** Says in the log when a copycat's finished model is not ours outside, so its decals cannot show. */
    @SubscribeEvent
    public static void checkModels(ModelEvent.BakingCompleted event) {
        List<String> lost = new ArrayList<>();
        for (Block block : copycats()) {
            BakedModel model = event.getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState());
            if (!(model instanceof AttachedDecalModel)) {
                lost.add(BuiltInRegistries.BLOCK.getKey(block) + " (" + model.getClass().getName() + ")");
            }
        }
        if (!lost.isEmpty()) {
            CreateDecals.LOGGER.warn("Decals: another model is outside ours, so decals will not show on: {}", String.join(", ", lost));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerColours(RegisterColorHandlersEvent.Block event) {
        List<Block> creates = copycats().stream().filter(AttachedCopycats::isCreates).toList();
        List<Block> others = copycats().stream().filter(block -> !AttachedCopycats.isCreates(block)).toList();
        register(event, CopycatBlock.wrappedColor(), creates);
        if (!others.isEmpty() && CopycatsPlus.loaded()) {
            register(event, CopycatsPlusClient.materialColour(), others);
        }
    }

    private static void register(RegisterColorHandlersEvent.Block event, BlockColor material, List<Block> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        event.register((state, level, pos, tintIndex) -> tintIndex >= DecalQuads.TINT_FLAG
                ? tintIndex & 0xFFFFFF
                : material.getColor(state, level, pos, tintIndex), blocks.toArray(Block[]::new));
    }
}
