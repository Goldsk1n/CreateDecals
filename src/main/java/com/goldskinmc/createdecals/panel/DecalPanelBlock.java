package com.goldskinmc.createdecals.panel;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.decal.DecalState;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Create's copycat panel with a decal on its front face. Everything about the material is Create's; a dye
 * recolours the decal. Which decal it shows is set with {@code /decals decal} for now.
 */
public class DecalPanelBlock extends CopycatPanelBlock {
    public DecalPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends CopycatBlockEntity> getBlockEntityType() {
        return CreateDecals.DECAL_PANEL_ENTITY.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof DyeItem dye && level.getBlockEntity(pos) instanceof DecalPanelBlockEntity be
                && be.getDecal() != null) {
            if (!level.isClientSide()) {
                DecalState decal = be.getDecal();
                be.setDecal(decal.withRgb(dye.getDyeColor().getTextureDiffuseColor()));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
