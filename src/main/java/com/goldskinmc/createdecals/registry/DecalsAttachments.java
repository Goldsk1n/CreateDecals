package com.goldskinmc.createdecals.registry;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.block.AttachedDecals;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The decals on other mods' copycats (Copycats+), kept beside their block entities. Registered with or without that mod. */
public final class DecalsAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CreateDecals.MOD_ID);

    public static final Supplier<AttachmentType<AttachedDecals>> DECALS = TYPES.register("decals",
            () -> AttachmentType.builder(() -> AttachedDecals.EMPTY).serialize(AttachedDecals.SERIALIZER).build());

    private DecalsAttachments() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
