package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.compat.CopycatsPlus;
import com.goldskinmc.createdecals.compat.copycats.CopycatsPlusClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * With Copycats+ installed, puts {@link AttachedDecalModel} round each of its copycats' models and gives them a colour
 * handler that knows decal tints. Both run last (lowest priority), after Create's model swapper and Copycats+ have set
 * up theirs, so ours wraps the finished model and replaces their colour handler with one that still calls theirs.
 */
@EventBusSubscriber(modid = CreateDecals.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CopycatsPlusModels {
    private CopycatsPlusModels() {
    }

    private static List<Block> copycats() {
        List<Block> blocks = new ArrayList<>();
        if (CopycatsPlus.loaded()) {
            for (Block block : BuiltInRegistries.BLOCK) {
                if (CopycatsPlus.takesDecals(block.defaultBlockState())) {
                    blocks.add(block);
                }
            }
        }
        return blocks;
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
            CreateDecals.LOGGER.info("Copycats+: decals on {} block states' models, such as {}", wrapped, example);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerColours(RegisterColorHandlersEvent.Block event) {
        List<Block> blocks = copycats();
        if (blocks.isEmpty()) {
            return;
        }
        BlockColor material = CopycatsPlusClient.materialColour();
        event.register((state, level, pos, tintIndex) -> tintIndex >= DecalQuads.TINT_FLAG
                ? tintIndex & 0xFFFFFF
                : material.getColor(state, level, pos, tintIndex), blocks.toArray(Block[]::new));
    }
}
