package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalState;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Create's Clipboard on decals: R-click copies the clicked face's decals (both layers), L-click pastes them onto another
 * face, under one key, so they go between our blocks and Copycats+'s alike. Our block entity is
 * {@link ClipboardCloneable} itself; Copycats+'s are Create {@link SmartBlockEntity}s, and Create's Clipboard asks each
 * of their behaviours too, so they are given a {@link Behaviour} (Create's public {@code attachBehaviourLate}) just
 * before a Clipboard reaches them ({@link #attachIfHeld}).
 */
public final class DecalClipboard {
    public static final String KEY = "createdecals_decal";

    private DecalClipboard() {
    }

    public static boolean write(DecalHolder holder, CompoundTag tag, Direction side) {
        DecalState back = holder.getDecal(side, false);
        DecalState front = holder.getDecal(side, true);
        if (back == null && front == null) {
            return false;
        }
        if (back != null) {
            tag.put("Decal", back.save());
        }
        if (front != null) {
            tag.put("Front", front.save());
        }
        return true;
    }

    public static boolean read(DecalHolder holder, CompoundTag tag, Direction side, boolean simulate) {
        if (!(tag.contains("Decal") || tag.contains("Front")) || !holder.acceptsDecalOn(side)) {
            return false;
        }
        if (!simulate) {
            holder.setDecal(side, false, tag.contains("Decal") ? DecalState.load(tag.getCompound("Decal")) : null);
            holder.setDecal(side, true, tag.contains("Front") ? DecalState.load(tag.getCompound("Front")) : null);
        }
        return true;
    }

    /** Gives an attached copycat our clipboard behaviour if the player holds a Clipboard and it has none yet (either side). */
    public static void attachIfHeld(ItemStack held, BlockEntity be) {
        if (AllBlocks.CLIPBOARD.isIn(held) && be instanceof SmartBlockEntity smart && AttachedCopycats.takesDecals(be.getBlockState())
                && smart.getBehaviour(Behaviour.TYPE) == null) {
            smart.attachBehaviourLate(new Behaviour(smart));
        }
    }

    /** The Clipboard's way onto an attached copycat's decals (kept in its attachment, {@link AttachedDecalHolder}). */
    public static final class Behaviour extends BlockEntityBehaviour implements ClipboardCloneable {
        public static final BehaviourType<Behaviour> TYPE = new BehaviourType<>("createdecals_decals");

        Behaviour(SmartBlockEntity be) {
            super(be);
        }

        @Override
        public BehaviourType<?> getType() {
            return TYPE;
        }

        @Override
        public String getClipboardKey() {
            return KEY;
        }

        @Override
        public boolean writeToClipboard(HolderLookup.Provider registries, CompoundTag tag, Direction side) {
            return DecalClipboard.write(new AttachedDecalHolder(blockEntity), tag, side);
        }

        @Override
        public boolean readFromClipboard(HolderLookup.Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
            return DecalClipboard.read(new AttachedDecalHolder(blockEntity), tag, side, simulate);
        }
    }
}
