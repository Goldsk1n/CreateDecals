package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalFrame;
import com.goldskinmc.createdecals.decal.DecalState;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
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
 * A copycat's block entity with decals, one per face at most: the Decal Panel uses only its front, the Decal Block all
 * six. Decals are saved with the block (so contraptions, Sable vehicles and schematics carry them like the material)
 * and handed to the model as model data; nothing ticks and nothing renders here.
 */
public class DecalCopycatBlockEntity extends CopycatBlockEntity {
    public static final ModelProperty<Map<Direction, DecalState>> DECALS = new ModelProperty<>();

    private final EnumMap<Direction, DecalState> decals = new EnumMap<>(Direction.class);

    public DecalCopycatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Map<Direction, DecalState> getDecals() {
        return Collections.unmodifiableMap(decals);
    }

    @Nullable
    public DecalState getDecal(Direction face) {
        return decals.get(face);
    }

    /** Sets or (with null) removes the decal on {@code face}; panels take only their front. */
    public void setDecal(Direction face, @Nullable DecalState decal) {
        if (!acceptsDecalOn(face) || Objects.equals(decals.get(face), decal)) {
            return;
        }
        if (decal == null) {
            decals.remove(face);
        } else {
            decals.put(face, decal);
        }
        changed();
    }

    /** Which faces can hold a decal: a panel's front, any face of a full block. */
    public boolean acceptsDecalOn(Direction face) {
        BlockState state = getBlockState();
        return !state.hasProperty(CopycatPanelBlock.FACING) || state.getValue(CopycatPanelBlock.FACING) == face;
    }

    private void changed() {
        if (level != null && !level.isClientSide()) {
            notifyUpdate();
        } else {
            redrawDecals();
        }
    }

    private void redrawDecals() {
        if (level == null) {
            return;
        }
        if (!isVirtual()) {
            requestModelDataUpdate();
        }
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
    }

    /**
     * A contraption put the block back turned or mirrored: the block state already faces the new way; move each decal
     * to the face it turned to, and turn it with the block (its top is carried through the same transform), or it would
     * stand upright again.
     */
    @Override
    public void transform(BlockEntity be, StructureTransform transform) {
        super.transform(be, transform);
        if (decals.isEmpty()) {
            return;
        }
        boolean mirrored = transform.mirror != null && transform.mirror != Mirror.NONE;
        EnumMap<Direction, DecalState> turned = new EnumMap<>(Direction.class);
        decals.forEach((face, decal) -> {
            Direction newFace = turn(transform, face);
            Integer rotation = DecalFrame.rotationFor(newFace, turn(transform, DecalFrame.top(face, decal.rotation())));
            turned.put(newFace, decal.withTurn(rotation != null ? rotation : decal.rotation(), mirrored != decal.flip()));
        });
        decals.clear();
        decals.putAll(turned);
        notifyUpdate();
    }

    /** What Create does to a block state's facing: mirror first, then rotate. */
    private static Direction turn(StructureTransform transform, Direction direction) {
        Direction mirrored = transform.mirrorFacing(direction);
        return transform.rotationAxis == null ? mirrored : transform.rotateFacing(mirrored);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        EnumMap<Direction, DecalState> before = new EnumMap<>(decals);
        decals.clear();
        if (tag.contains("Decals")) {
            CompoundTag faces = tag.getCompound("Decals");
            for (Direction face : Direction.values()) {
                if (faces.contains(face.getName())) {
                    decals.put(face, DecalState.load(faces.getCompound(face.getName())));
                }
            }
        } else if (tag.contains("Decal") && getBlockState().hasProperty(CopycatPanelBlock.FACING)) {
            // A prototype panel: its one decal was on the front
            decals.put(getBlockState().getValue(CopycatPanelBlock.FACING), DecalState.load(tag.getCompound("Decal")));
        }
        if (clientPacket && !before.equals(decals)) {
            redrawDecals();
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        writeDecals(tag);
    }

    /** Schematics: the decals are kept, they cost nothing. */
    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        writeDecals(tag);
    }

    private void writeDecals(CompoundTag tag) {
        if (decals.isEmpty()) {
            return;
        }
        CompoundTag faces = new CompoundTag();
        decals.forEach((face, decal) -> faces.put(face.getName(), decal.save()));
        tag.put("Decals", faces);
    }

    @Override
    public ModelData getModelData() {
        ModelData.Builder builder = ModelData.builder().with(CopycatModel.MATERIAL_PROPERTY, getMaterial());
        if (!decals.isEmpty()) {
            builder.with(DECALS, Map.copyOf(decals));
        }
        return builder.build();
    }
}
