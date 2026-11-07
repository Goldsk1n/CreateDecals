package com.goldskinmc.createdecals.ponder;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.registry.DecalsBlocks;
import com.google.common.hash.Hashing;
import com.simibubi.create.content.decoration.copycat.CopycatPanelBlock;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Writes the Ponder scene schematics ({@code assets/createdecals/ponder/*.nbt}) at datagen, in the structure-template
 * format Ponder loads, from the {@link Layouts}: change a layout, rerun {@code runData}, commit {@code src/generated/}.
 * Copycats are saved with a material and its item (Create resets a material it cannot match to an item); scenes add
 * the decals as they go.
 */
public final class PonderSchematics implements DataProvider {
    private final PackOutput.PathProvider paths;

    private PonderSchematics(PackOutput output) {
        paths = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "ponder");
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new PonderSchematics(event.getGenerator().getPackOutput()));
    }

    static List<Schematic> all() {
        List<Schematic> all = new ArrayList<>();
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();

        Schematic panel = new Schematic("decal_panel", Layouts.Panel.SIZE, 4);
        panel.fill(Layouts.Panel.WALL_FROM, Layouts.Panel.WALL_TO, Blocks.STONE_BRICKS.defaultBlockState());
        panel.copycat(Layouts.Panel.PANEL, DecalsBlocks.DECAL_PANEL.getDefaultState().setValue(CopycatPanelBlock.FACING, Direction.NORTH), null);
        all.add(panel);

        Schematic block = new Schematic("decal_block", Layouts.Block.SIZE, 4);
        block.copycat(Layouts.Block.BLOCK, DecalsBlocks.DECAL_BLOCK.getDefaultState(), white);
        all.add(block);

        Schematic stencil = new Schematic("decal_stencil", Layouts.Stencil.SIZE, 5);
        BlockPos.betweenClosed(Layouts.Stencil.WALL_FROM, Layouts.Stencil.WALL_TO)
                .forEach(pos -> stencil.copycat(pos.immutable(), DecalsBlocks.DECAL_BLOCK.getDefaultState(), white));
        stencil.copycat(Layouts.Stencil.SIDE, DecalsBlocks.DECAL_BLOCK.getDefaultState(), Blocks.BLACK_CONCRETE.defaultBlockState());
        all.add(stencil);
        return all;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (Schematic schematic : all()) {
            byte[] bytes = schematic.toBytes();
            writes.add(CompletableFuture.runAsync(() -> {
                try {
                    output.writeIfNeeded(paths.file(CreateDecals.id(schematic.name), "nbt"), bytes, Hashing.sha1().hashBytes(bytes));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, Util.backgroundExecutor()));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Create: Decals Ponder schematics";
    }

    /** One scene's blocks on a checkered base plate, saved like a structure block would. */
    static final class Schematic {
        final String name;
        private final int size;
        private final int height;
        private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        private final Map<BlockPos, CompoundTag> blockEntities = new LinkedHashMap<>();

        Schematic(String name, int size, int height) {
            this.name = name;
            this.size = size;
            this.height = height;
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    set(new BlockPos(x, 0, z), ((x + z) % 2 == 0 ? Blocks.SNOW_BLOCK : Blocks.WHITE_CONCRETE_POWDER).defaultBlockState());
                }
            }
        }

        void set(BlockPos pos, BlockState state) {
            blocks.put(pos, state);
        }

        void fill(BlockPos from, BlockPos to, BlockState state) {
            BlockPos.betweenClosed(from, to).forEach(pos -> set(pos.immutable(), state));
        }

        /** A decal copycat, copying {@code material} (null: still the bare copycat). */
        void copycat(BlockPos pos, BlockState state, @Nullable BlockState material) {
            set(pos, state);
            CompoundTag tag = new CompoundTag();
            tag.putString("id", CreateDecals.id("decal_copycat").toString());
            if (material != null) {
                tag.put("Material", NbtUtils.writeBlockState(material));
                CompoundTag item = new CompoundTag();
                item.putString("id", BuiltInRegistries.ITEM.getKey(material.getBlock().asItem()).toString());
                item.putInt("count", 1);
                tag.put("Item", item);
            }
            blockEntities.put(pos, tag);
        }

        byte[] toBytes() {
            List<BlockState> palette = new ArrayList<>();
            ListTag blockList = new ListTag();
            blocks.forEach((pos, state) -> {
                int index = palette.indexOf(state);
                if (index < 0) {
                    index = palette.size();
                    palette.add(state);
                }
                CompoundTag block = new CompoundTag();
                block.put("pos", ints(pos.getX(), pos.getY(), pos.getZ()));
                block.putInt("state", index);
                CompoundTag blockEntity = blockEntities.get(pos);
                if (blockEntity != null) {
                    block.put("nbt", blockEntity.copy());
                }
                blockList.add(block);
            });
            ListTag paletteList = new ListTag();
            palette.forEach(state -> paletteList.add(NbtUtils.writeBlockState(state)));
            CompoundTag root = new CompoundTag();
            root.put("size", ints(size, height, size));
            root.put("palette", paletteList);
            root.put("blocks", blockList);
            root.put("entities", new ListTag());
            NbtUtils.addCurrentDataVersion(root);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try {
                NbtIo.writeCompressed(root, bytes);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return bytes.toByteArray();
        }

        private static ListTag ints(int... values) {
            ListTag list = new ListTag();
            for (int value : values) {
                list.add(IntTag.valueOf(value));
            }
            return list;
        }
    }
}
