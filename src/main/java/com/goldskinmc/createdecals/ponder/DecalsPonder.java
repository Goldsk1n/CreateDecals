package com.goldskinmc.createdecals.ponder;

import com.goldskinmc.createdecals.CreateDecals;
import java.util.function.BiConsumer;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;

/**
 * Our Ponder plugin, hooked in on the client at setup and at datagen to collect the scene text (client setup does not
 * run there). Client and datagen only.
 */
public final class DecalsPonder implements PonderPlugin {
    private static boolean registered;

    public static void registerPlugin() {
        if (!registered) {
            registered = true;
            PonderIndex.addPlugin(new DecalsPonder());
        }
    }

    /** English scene titles, text and tag names, for the lang provider. */
    public static void provideLang(BiConsumer<String, String> consumer) {
        registerPlugin();
        PonderIndex.getLangAccess().provideLang(CreateDecals.MOD_ID, consumer);
    }

    @Override
    public String getModId() {
        return CreateDecals.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        DecalsScenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        DecalsScenes.registerTags(helper);
    }
}
