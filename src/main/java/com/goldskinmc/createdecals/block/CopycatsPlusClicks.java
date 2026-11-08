package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.compat.CopycatsPlus;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Dye, a Glow Ink Sac and an Ink Sac on a decal on one of Copycats+'s copycats do what they do on ours
 * ({@link DecalClicks}); our blocks handle these clicks themselves, its copycats are not ours to change, so they are
 * caught before the block sees them. Clicks on faces without a decal go on to the copycat as before. A Clipboard in hand
 * first gives the copycat our clipboard behaviour, so Create's Clipboard handler, which runs next, finds it.
 */
@EventBusSubscriber(modid = CreateDecals.MOD_ID)
public final class CopycatsPlusClicks {
    private CopycatsPlusClicks() {
    }

    /** Before Create's Clipboard handler, on both sides: a Copycats+ copycat gets our clipboard behaviour (DecalClipboard). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeClipboardCopy(PlayerInteractEvent.RightClickBlock event) {
        attachClipboard(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeClipboardPaste(PlayerInteractEvent.LeftClickBlock event) {
        attachClipboard(event);
    }

    private static void attachClipboard(PlayerInteractEvent event) {
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be != null) {
            DecalClipboard.attachIfHeld(event.getItemStack(), be);
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getFace() == null || !CopycatsPlus.takesDecals(event.getLevel().getBlockState(event.getPos()))) {
            return;
        }
        ItemInteractionResult used = DecalClicks.use(event.getItemStack(), event.getLevel(), event.getPos(), event.getFace(), event.getEntity());
        if (used != null) {
            event.setCanceled(true);
            event.setCancellationResult(used.result());
        }
    }
}
