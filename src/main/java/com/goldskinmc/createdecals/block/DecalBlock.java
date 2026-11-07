package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.registry.DecalsBlockEntities;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A full-cube copycat with a decal slot on each face, for hulls built of whole blocks (a panel on them would stick out
 * three pixels). Create has no full copycat block of its own; this one follows its panel: the material decides how it
 * looks, sounds and breaks, and faces against solid neighbours are hidden.
 */
public class DecalBlock extends CopycatBlock {
    public DecalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends CopycatBlockEntity> getBlockEntityType() {
        return DecalsBlockEntities.DECAL_COPYCAT.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult dyed = DecalClicks.use(stack, level, pos, hit.getDirection());
        return dyed != null ? dyed : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** Connected textures (casings) join across every face, as between real blocks. */
    @Override
    public boolean canConnectTexturesToward(BlockAndTintGetter reader, BlockPos fromPos, BlockPos toPos, BlockState state) {
        return true;
    }

    @Override
    public boolean canFaceBeOccluded(BlockState state, Direction face) {
        return true;
    }

    @Override
    public boolean supportsExternalFaceHiding(BlockState state) {
        return true;
    }

    /**
     * Copycats let light and neighbours' faces through (Create gives them no occlusion, since the material may be
     * glass), so a solid material hides the neighbour's face against it here instead, and glass hides glass.
     */
    @Override
    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
        BlockState material = getMaterial(level, pos);
        if (material.isSolidRender(level, pos)) {
            return true;
        }
        BlockState neighbourMaterial = neighborState.is(this) ? getMaterial(level, pos.relative(dir)) : neighborState;
        return material.skipRendering(neighbourMaterial, dir);
    }
}
