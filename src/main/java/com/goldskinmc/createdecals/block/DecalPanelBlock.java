package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.registry.DecalsBlockEntities;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Create's copycat panel with a decal on its front face; everything about the material is Create's. */
public class DecalPanelBlock extends CopycatPanelBlock {
    public DecalPanelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends CopycatBlockEntity> getBlockEntityType() {
        return DecalsBlockEntities.DECAL_COPYCAT.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult dyed = DecalClicks.dye(stack, level, pos, state.getValue(FACING));
        return dyed != null ? dyed : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
