package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.registry.DecalsBlockEntities;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatStepBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Create's copycat step with decals on its faces: each face shows the part of the decal it covers, as if the decal lay on
 * the block's face in that plane, so steps line up with Decal Blocks beside them. Everything about the material is
 * Create's.
 */
public class DecalStepBlock extends CopycatStepBlock {
    public DecalStepBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends CopycatBlockEntity> getBlockEntityType() {
        return DecalsBlockEntities.DECAL_COPYCAT.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult used = DecalClicks.use(stack, level, pos, hit.getDirection(), player);
        return used != null ? used : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
