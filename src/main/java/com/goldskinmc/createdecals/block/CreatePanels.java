package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

/**
 * Create's own Copycat Panel takes a decal by becoming a Decal Panel where it stands: same facing, waterlogging, material
 * and consumed item, so builds made of Create's panels need no rebuilding. Only Create's panel itself, not blocks
 * extending it (ours, other add-ons'), and only on its front. Create has no full copycat block, so there is nothing to
 * turn into a Decal Block.
 */
public final class CreatePanels {
    private CreatePanels() {
    }

    /** Whether a decal on {@code face} of this block would turn it into a Decal Panel. */
    public static boolean convertible(BlockState state, Direction face) {
        return state.is(AllBlocks.COPYCAT_PANEL.get()) && state.getValue(CopycatPanelBlock.FACING) == face;
    }

    /** Whether this is Create's own Copycat Panel, whichever way it faces. */
    public static boolean isCreatePanel(BlockState state) {
        return state.is(AllBlocks.COPYCAT_PANEL.get());
    }

    /**
     * Turns Create's panel at {@code pos} into a Decal Panel (server side) and returns its block entity, or null when
     * {@link #convertible} says no.
     */
    @Nullable
    public static DecalCopycatBlockEntity convert(Level level, BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);
        if (level.isClientSide() || !convertible(state, face) || !(level.getBlockEntity(pos) instanceof CopycatBlockEntity old)) {
            return null;
        }
        // The saved data carries the material as it is (Create's setMaterial may take a neighbour's state instead); the
        // old panel's item is emptied first, or Create would drop it when the block changes
        CompoundTag data = old.saveWithoutMetadata(level.registryAccess());
        old.setConsumedItem(ItemStack.EMPTY);
        level.setBlock(pos, copyProperties(state, DecalsBlocks.DECAL_PANEL.getDefaultState()), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof DecalCopycatBlockEntity be)) {
            return null;
        }
        be.loadWithComponents(data, level.registryAccess());
        be.notifyUpdate();
        return be;
    }

    private static BlockState copyProperties(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties()) {
            to = copy(from, to, property);
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }
}
