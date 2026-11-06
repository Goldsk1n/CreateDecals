package com.goldskinmc.livery.client;

import com.goldskinmc.livery.Livery;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

/** Puts {@link DecalPanelModel} round the panel's (empty) block model and tints decal quads by their tint index. */
@EventBusSubscriber(modid = Livery.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LiveryClient {
    private LiveryClient() {
    }

    @SubscribeEvent
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        for (BlockState state : Livery.DECAL_PANEL.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            BakedModel model = event.getModels().get(location);
            if (model != null && !(model instanceof DecalPanelModel)) {
                event.getModels().put(location, new DecalPanelModel(model));
            }
        }
    }

    @SubscribeEvent
    public static void onAtlasStitched(TextureAtlasStitchedEvent event) {
        DecalPanelModel.clearCache();
    }

    @SubscribeEvent
    public static void registerColours(RegisterColorHandlersEvent.Block event) {
        BlockColor material = CopycatBlock.wrappedColor();
        event.register((state, level, pos, tintIndex) -> tintIndex >= DecalPanelModel.TINT_FLAG
                ? tintIndex & 0xFFFFFF
                : material.getColor(state, level, pos, tintIndex), Livery.DECAL_PANEL.get());
    }
}
