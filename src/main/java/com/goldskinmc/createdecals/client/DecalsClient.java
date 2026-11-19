package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.ponder.DecalsPonder;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.goldskinmc.createdecals.registry.DecalsItems;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import java.util.function.Function;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

/** Puts the decal model round the Decal Block's (empty) block model, tints decals and the stencil, loads the decal library. */
@EventBusSubscriber(modid = CreateDecals.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DecalsClient {
    private DecalsClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        DecalsPonder.registerPlugin();
    }

    @SubscribeEvent
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        wrap(event, DecalsBlocks.DECAL_BLOCK.get(), DecalBlockModel::new);
        ModelResourceLocation stencil = ModelResourceLocation.inventory(DecalsItems.DECAL_STENCIL.getId());
        BakedModel stencilModel = event.getModels().get(stencil);
        if (stencilModel != null) {
            event.getModels().put(stencil, new StencilItemModel(stencilModel, event.getModels().get(StencilItemModel.ERASER)));
        }
    }

    /** The stencil's eraser look, baked with the other models though no item names it. */
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(StencilItemModel.ERASER);
    }

    private static void wrap(ModelEvent.ModifyBakingResult event, Block block, Function<BakedModel, BakedModel> wrapper) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            BakedModel model = event.getModels().get(location);
            if (model != null) {
                event.getModels().put(location, wrapper.apply(model));
            }
        }
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(DecalLibrary.INSTANCE);
    }

    @SubscribeEvent
    public static void onAtlasStitched(TextureAtlasStitchedEvent event) {
        DecalQuads.clearCache();
        StencilItemModel.clearCache();
    }

    /** The stencil's icon: a chosen decal ({@link StencilItemModel}) carries its colour in the tint index, as on blocks. */
    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            return layer >= DecalQuads.TINT_FLAG ? 0xFF000000 | layer & 0xFFFFFF : -1;
        }, DecalsItems.DECAL_STENCIL.get());
    }

    /** Decal quads carry their colour in the tint index; everything else is the copied material's tint (grass, leaves). */
    @SubscribeEvent
    public static void registerColours(RegisterColorHandlersEvent.Block event) {
        BlockColor material = CopycatBlock.wrappedColor();
        event.register((state, level, pos, tintIndex) -> tintIndex >= DecalQuads.TINT_FLAG
                ? tintIndex & 0xFFFFFF
                : material.getColor(state, level, pos, tintIndex), DecalsBlocks.DECAL_BLOCK.get());
    }
}
