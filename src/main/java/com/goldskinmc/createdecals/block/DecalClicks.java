package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalState;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Clicks both decal blocks share, on the decal seen on top of the clicked face. */
final class DecalClicks {
    private DecalClicks() {
    }

    /**
     * A dye on a face with a decal recolours that decal, every tile of a large one; a Glow Ink Sac makes it glow and an
     * Ink Sac stops it glowing, as on signs. Nothing is used up (Decision 8 of the plan: decals cost nothing).
     * Null when the click is not for us, so the copycat handles it.
     */
    @Nullable
    static ItemInteractionResult use(ItemStack stack, Level level, BlockPos pos, Direction face, @Nullable Player player) {
        UnaryOperator<DecalState> change;
        if (stack.getItem() instanceof DyeItem dye) {
            int rgb = dye.getDyeColor().getTextureDiffuseColor();
            change = tile -> tile.withRgb(rgb);
        } else if (stack.is(Items.GLOW_INK_SAC)) {
            change = tile -> tile.withGlow(true);
        } else if (stack.is(Items.INK_SAC)) {
            change = tile -> tile.withGlow(false);
        } else {
            return null;
        }
        if (!(level.getBlockEntity(pos) instanceof DecalCopycatBlockEntity be) || be.topLayer(face) == null) {
            return null;
        }
        if (!level.isClientSide()) {
            DecalTiles.changeWhole(level, pos, face, change, DecalEdits.by(player, level, face, stack));
            if (!stack.is(Items.GLOW_INK_SAC) && !stack.is(Items.INK_SAC)) {
                return ItemInteractionResult.SUCCESS;
            }
            level.playSound(null, pos, stack.is(Items.GLOW_INK_SAC) ? SoundEvents.GLOW_INK_SAC_USE : SoundEvents.INK_SAC_USE,
                    SoundSource.BLOCKS, 1, 1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
