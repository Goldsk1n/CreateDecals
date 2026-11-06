package com.goldskinmc.livery;

import com.goldskinmc.livery.command.LiveryCommand;
import com.goldskinmc.livery.panel.DecalPanelBlock;
import com.goldskinmc.livery.panel.DecalPanelBlockEntity;
import com.mojang.logging.LogUtils;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Livery.MOD_ID)
public final class Livery {
    public static final String MOD_ID = "livery";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    /** Properties as Create gives its copycats (soft metal, no occlusion, nothing spawns on it). */
    public static final DeferredBlock<DecalPanelBlock> DECAL_PANEL = BLOCKS.register("decal_panel",
            () -> new DecalPanelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .strength(2f, 5f)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)));
    public static final DeferredItem<BlockItem> DECAL_PANEL_ITEM = ITEMS.registerSimpleBlockItem(DECAL_PANEL);
    public static final Supplier<BlockEntityType<DecalPanelBlockEntity>> DECAL_PANEL_ENTITY = BLOCK_ENTITIES.register("decal_panel",
            () -> BlockEntityType.Builder.of(
                    (pos, state) -> new DecalPanelBlockEntity(Livery.DECAL_PANEL_ENTITY.get(), pos, state),
                    DECAL_PANEL.get()).build(null));

    public Livery(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(Livery::addToCreativeTab);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> LiveryCommand.register(event.getDispatcher()));
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(DECAL_PANEL_ITEM.get());
        }
    }
}
