package com.goldskinmc.createdecals.block;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.contraption.DecalMovement;
import com.goldskinmc.createdecals.decal.DecalFrame;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.registry.DecalsAttachments;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.api.schematic.nbt.PartialSafeNBT;
import com.simibubi.create.api.schematic.nbt.SafeNbtWriterRegistry;
import com.simibubi.create.content.contraptions.StructureTransform;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.attachment.AttachmentHolder;

/**
 * Decals carried through Create's moves: a contraption putting a block back turned or mirrored ({@link #turned}, for our
 * Decal Block and the attached copycats), and Create's schematics, which save Create's and Copycats+'s copycats with
 * their own {@code writeSafe} ({@link #writeSafe}). For the attached copycats both hook in through Create's own public
 * API ({@link #registerAttached}), not into Create's or Copycats+'s blocks.
 */
public final class DecalTransforms {
    private DecalTransforms() {
    }

    /**
     * One layer of decals after the block turned: each to the face it turned to, and turned with the block (its top is
     * carried through the same transform), or it would stand upright again; mirrored, flipped.
     */
    public static Map<Direction, DecalState> turned(Map<Direction, DecalState> layer, StructureTransform transform) {
        boolean mirrored = transform.mirror != null && transform.mirror != Mirror.NONE;
        EnumMap<Direction, DecalState> turned = new EnumMap<>(Direction.class);
        layer.forEach((face, decal) -> {
            Direction newFace = turn(transform, face);
            Integer rotation = DecalFrame.rotationFor(newFace, turn(transform, DecalFrame.top(face, decal.rotation())));
            turned.put(newFace, decal.withTurn(rotation != null ? rotation : decal.rotation(), mirrored != decal.flip()));
        });
        return turned;
    }

    /** What Create does to a block state's facing: mirror first, then rotate. */
    private static Direction turn(StructureTransform transform, Direction direction) {
        Direction mirrored = transform.mirrorFacing(direction);
        return transform.rotationAxis == null ? mirrored : transform.rotateFacing(mirrored);
    }

    /** An attached copycat put back turned or mirrored by a contraption or a schematic: its decals turn with it. */
    public static void turnAttached(BlockEntity be, StructureTransform transform) {
        be.getExistingData(DecalsAttachments.DECALS.get()).filter(decals -> !decals.isEmpty()).ifPresent(decals ->
                AttachedDecalHolder.set(be, new AttachedDecals(turned(decals.back(), transform), turned(decals.front(), transform))));
    }

    /**
     * Hooks the attached copycats' block entity types (Create's copycat, Copycats+'s) into Create's own public registries
     * for moved and schematic block entities ({@code MovedBlockTransformerRegistries}, {@code SafeNbtWriterRegistry}),
     * which Create asks before a block entity's own {@code transform} and {@code writeSafe}: each calls the block entity's
     * own method first, then turns or saves the decals. No mixin; if another mod has already registered one for a type,
     * that type keeps it and its decals do not turn or reach schematics (logged).
     */
    public static void registerAttached() {
        List<Block> copycats = AttachedCopycats.blocks();
        Set<BlockEntityType<?>> types = new LinkedHashSet<>();
        for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            for (Block block : copycats) {
                if (type.isValid(block.defaultBlockState())) {
                    types.add(type);
                    break;
                }
            }
        }
        int hooked = 0;
        for (BlockEntityType<?> type : types) {
            try {
                MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(type, (be, transform) -> {
                    if (be instanceof TransformableBlockEntity own) {
                        own.transform(be, transform);
                    }
                    turnAttached(be, transform);
                });
                SafeNbtWriterRegistry.REGISTRY.register(type, (be, tag, registries) -> {
                    if (be instanceof PartialSafeNBT own) {
                        own.writeSafe(tag, registries);
                    }
                    writeSafe(be, tag, registries);
                });
                hooked++;
            } catch (IllegalArgumentException taken) {
                CreateDecals.LOGGER.warn("Decals: another mod already handles moving or saving {}; its decals stay as they are there",
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type));
            }
        }
        CreateDecals.LOGGER.info("Decals: decals turn with contraptions and go into schematics on {} block entity types", hooked);
        // Glowing decals on moving contraptions are drawn again at full light by our movement behaviour (Sodium drops the
        // mesh's baked light): given to the attached copycats that have none of their own. Each becomes a contraption
        // actor, about 0.2 microseconds a tick each (measured Nov 7)
        int moving = 0;
        for (Block block : copycats) {
            if (MovementBehaviour.REGISTRY.get(block) == null) {
                MovementBehaviour.REGISTRY.register(block, new DecalMovement());
                moving++;
            }
        }
        CreateDecals.LOGGER.info("Decals: glowing decals full-bright on contraptions for {} copycats", moving);
    }

    /**
     * Create saves block entities into schematics with {@code writeSafe}, which leaves NeoForge's attachments
     * out; ours are put in where NeoForge keeps them, so the Schematicannon's block loads them as it loads a chunk's.
     */
    public static void writeSafe(BlockEntity be, CompoundTag tag, HolderLookup.Provider registries) {
        be.getExistingData(DecalsAttachments.DECALS.get()).filter(decals -> !decals.isEmpty()).ifPresent(decals -> {
            CompoundTag attachments = tag.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY);
            attachments.put(CreateDecals.id("decals").toString(), decals.save());
            tag.put(AttachmentHolder.ATTACHMENTS_NBT_KEY, attachments);
        });
    }
}
