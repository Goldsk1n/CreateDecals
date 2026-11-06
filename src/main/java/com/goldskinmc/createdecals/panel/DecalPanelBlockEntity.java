package com.goldskinmc.createdecals.panel;

import com.goldskinmc.createdecals.decal.DecalFrame;
import com.goldskinmc.createdecals.decal.DecalState;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A copycat panel's block entity with one decal. The decal is saved with the block (so contraptions, Sable
 * vehicles and schematics carry it like the material) and handed to the model as model data; nothing ticks
 * and nothing renders here.
 */
public class DecalPanelBlockEntity extends CopycatBlockEntity {
    public static final ModelProperty<DecalState> DECAL = new ModelProperty<>();

    @Nullable
    private DecalState decal;

    public DecalPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public DecalState getDecal() {
        return decal;
    }

    public void setDecal(@Nullable DecalState newDecal) {
        if (Objects.equals(decal, newDecal)) {
            return;
        }
        decal = newDecal;
        if (level != null && !level.isClientSide()) {
            notifyUpdate();
        } else {
            redrawDecal();
        }
    }

    private void redrawDecal() {
        if (level == null) {
            return;
        }
        if (!isVirtual()) {
            requestModelDataUpdate();
        }
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
    }

    /**
     * A contraption that turned or mirrored the panel before putting it back: the block state already faces the new
     * way, so turn the decal with it (its top is carried through the same transform), or it would stand upright again.
     */
    @Override
    public void transform(BlockEntity be, StructureTransform transform) {
        super.transform(be, transform);
        if (decal == null || !getBlockState().hasProperty(CopycatPanelBlock.FACING)) {
            return;
        }
        Direction facing = getBlockState().getValue(CopycatPanelBlock.FACING);
        // Undo the transform on the new facing to find the old one: rotate back, then mirror (its own inverse)
        Direction oldFacing = facing;
        if (transform.rotationAxis != null) {
            for (int i = 0; i < (4 - transform.rotation.ordinal()) % 4; i++) {
                oldFacing = oldFacing.getClockWise(transform.rotationAxis);
            }
        }
        oldFacing = transform.mirrorFacing(oldFacing);
        Direction top = transform.rotateFacing(transform.mirrorFacing(DecalFrame.top(oldFacing, decal.rotation())));
        Integer rotation = DecalFrame.rotationFor(facing, top);
        if (rotation == null) {
            return;
        }
        boolean flip = transform.mirror != null && transform.mirror != Mirror.NONE ? !decal.flip() : decal.flip();
        decal = new DecalState(decal.decal(), decal.rgb(), rotation, flip);
        notifyUpdate();
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        DecalState before = decal;
        decal = tag.contains("Decal") ? DecalState.load(tag.getCompound("Decal")) : null;
        if (clientPacket && !Objects.equals(before, decal)) {
            redrawDecal();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        writeDecal(tag);
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        writeDecal(tag);
    }

    private void writeDecal(CompoundTag tag) {
        if (decal != null) {
            tag.put("Decal", decal.save());
        }
    }

    @Override
    public ModelData getModelData() {
        ModelData.Builder builder = ModelData.builder().with(CopycatModel.MATERIAL_PROPERTY, getMaterial());
        if (decal != null) {
            builder.with(DECAL, decal);
        }
        return builder.build();
    }
}
