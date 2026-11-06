package com.goldskinmc.createdecals.registry;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.stencil.StencilData;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components: what a Decal Stencil holds. */
public final class DecalsComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateDecals.MOD_ID);

    public static final Supplier<DataComponentType<StencilData>> STENCIL = COMPONENTS.register("stencil",
            () -> DataComponentType.<StencilData>builder().persistent(StencilData.CODEC).networkSynchronized(StencilData.STREAM_CODEC).build());

    private DecalsComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
