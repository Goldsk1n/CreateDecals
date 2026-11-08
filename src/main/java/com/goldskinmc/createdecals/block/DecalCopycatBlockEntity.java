package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.decal.DecalFrame;
import com.goldskinmc.createdecals.decal.DecalState;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A copycat's block entity with decals: each face holds a back decal and a front one drawn over it (a number on a
 * stripe); the Decal Panel uses only its front face, the Decal Block all six. Decals are saved with the block (so
 * contraptions, Sable vehicles and schematics carry them like the material) and handed to the model as model data;
 * nothing ticks and nothing renders here. Methods without a layer work on the back one.
 */
public class DecalCopycatBlockEntity extends CopycatBlockEntity implements ClipboardCloneable, DecalHolder {
    public static final ModelProperty<Map<Direction, DecalState>> DECALS = new ModelProperty<>();
    public static final ModelProperty<Map<Direction, DecalState>> FRONT_DECALS = new ModelProperty<>();

    private final EnumMap<Direction, DecalState> decals = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, DecalState> fronts = new EnumMap<>(Direction.class);

    public DecalCopycatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Map<Direction, DecalState> getDecals() {
        return Collections.unmodifiableMap(decals);
    }

    public Map<Direction, DecalState> getFrontDecals() {
        return Collections.unmodifiableMap(fronts);
    }

    @Nullable
    public DecalState getDecal(Direction face) {
        return decals.get(face);
    }

    @Override
    @Nullable
    public DecalState getDecal(Direction face, boolean front) {
        return (front ? fronts : decals).get(face);
    }

    /**
     * Which layer of {@code face} is seen on top: true for the front one, false for the back one, null for a bare face.
     * Picking up, the eraser, dye and glow work on it.
     */
    @Override
    @Nullable
    public Boolean topLayer(Direction face) {
        return fronts.containsKey(face) ? Boolean.TRUE : decals.containsKey(face) ? Boolean.FALSE : null;
    }

    /**
     * The layer a stencil set to the {@code front} or back layer works on at {@code face} (picking up, erasing): that layer
     * if it has a decal, else the other one; null for a bare face.
     */
    @Override
    @Nullable
    public Boolean layerFor(Direction face, boolean front) {
        if ((front ? fronts : decals).containsKey(face)) {
            return front;
        }
        return (front ? decals : fronts).containsKey(face) ? !front : null;
    }

    /** Sets or (with null) removes the back decal on {@code face}; panels take only their front face. */
    public void setDecal(Direction face, @Nullable DecalState decal) {
        setDecal(face, false, decal);
    }

    /** Sets or (with null) removes the decal in one layer of {@code face}. */
    @Override
    public void setDecal(Direction face, boolean front, @Nullable DecalState decal) {
        EnumMap<Direction, DecalState> layer = front ? fronts : decals;
        if (!acceptsDecalOn(face) || Objects.equals(layer.get(face), decal)) {
            return;
        }
        if (decal == null) {
            layer.remove(face);
        } else {
            layer.put(face, decal);
        }
        changed();
    }

    /** Which faces can hold a decal: a panel's front, any face of a full block. */
    @Override
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
        if (decals.isEmpty() && fronts.isEmpty()) {
            return;
        }
        turnLayer(decals, transform);
        turnLayer(fronts, transform);
        notifyUpdate();
    }

    private static void turnLayer(EnumMap<Direction, DecalState> layer, StructureTransform transform) {
        Map<Direction, DecalState> turned = DecalTransforms.turned(layer, transform);
        layer.clear();
        layer.putAll(turned);
    }

    /** Create's Clipboard: R-click copies the clicked face's decals (both layers), L-click pastes them onto another face. */
    @Override
    public String getClipboardKey() {
        return "createdecals_decal";
    }

    @Override
    public boolean writeToClipboard(HolderLookup.Provider registries, CompoundTag tag, Direction side) {
        DecalState back = decals.get(side);
        DecalState front = fronts.get(side);
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

    @Override
    public boolean readFromClipboard(HolderLookup.Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
        if (!(tag.contains("Decal") || tag.contains("Front")) || !acceptsDecalOn(side)) {
            return false;
        }
        if (!simulate) {
            setDecal(side, false, tag.contains("Decal") ? DecalState.load(tag.getCompound("Decal")) : null);
            setDecal(side, true, tag.contains("Front") ? DecalState.load(tag.getCompound("Front")) : null);
        }
        return true;
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        EnumMap<Direction, DecalState> before = new EnumMap<>(decals);
        EnumMap<Direction, DecalState> frontsBefore = new EnumMap<>(fronts);
        readLayer(tag, "FrontDecals", fronts);
        readLayer(tag, "Decals", decals);
        if (!tag.contains("Decals") && tag.contains("Decal") && getBlockState().hasProperty(CopycatPanelBlock.FACING)) {
            // A prototype panel: its one decal was on the front
            decals.put(getBlockState().getValue(CopycatPanelBlock.FACING), DecalState.load(tag.getCompound("Decal")));
        }
        if (clientPacket && !(before.equals(decals) && frontsBefore.equals(fronts))) {
            redrawDecals();
        }
    }

    private static void readLayer(CompoundTag tag, String key, EnumMap<Direction, DecalState> layer) {
        layer.clear();
        CompoundTag faces = tag.getCompound(key);
        for (Direction face : Direction.values()) {
            if (faces.contains(face.getName())) {
                layer.put(face, DecalState.load(faces.getCompound(face.getName())));
            }
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
        writeLayer(tag, "Decals", decals);
        writeLayer(tag, "FrontDecals", fronts);
    }

    private static void writeLayer(CompoundTag tag, String key, EnumMap<Direction, DecalState> layer) {
        if (layer.isEmpty()) {
            return;
        }
        CompoundTag faces = new CompoundTag();
        layer.forEach((face, decal) -> faces.put(face.getName(), decal.save()));
        tag.put(key, faces);
    }

    @Override
    public ModelData getModelData() {
        ModelData.Builder builder = ModelData.builder().with(CopycatModel.MATERIAL_PROPERTY, getMaterial());
        if (!decals.isEmpty()) {
            builder.with(DECALS, Map.copyOf(decals));
        }
        if (!fronts.isEmpty()) {
            builder.with(FRONT_DECALS, Map.copyOf(fronts));
        }
        return builder.build();
    }
}
