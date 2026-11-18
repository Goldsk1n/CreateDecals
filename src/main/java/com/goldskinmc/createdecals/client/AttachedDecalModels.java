package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.AttachedCopycats;
import net.minecraft.core.registries.BuiltInRegistries;
import com.goldskinmc.createdecals.compat.CopycatsPlus;
import com.goldskinmc.createdecals.compat.copycats.CopycatsPlusClient;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import java.util.List;
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

/**
 * Puts {@link AttachedDecalModel} round each attached copycat's model (Create's Copycat Panel and Step, Copycats+'s
 * copycats: {@link AttachedCopycats}) and gives them a colour handler that knows decal tints. Both run last (lowest
 * priority), after Create's model swapper and Copycats+ have set up theirs, so ours wraps the finished model and replaces
 * their colour handler with one that still calls theirs.
 */
@EventBusSubscriber(modid = CreateDecals.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AttachedDecalModels {
    private AttachedDecalModels() {
    }

    private static List<Block> copycats() {
        return AttachedCopycats.blocks();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        int wrapped = 0;
        String example = null;
        for (Block block : copycats()) {
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
