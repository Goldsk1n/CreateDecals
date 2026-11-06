package com.goldskinmc.createdecals.command;

import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.decal.Decals;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/**
 * Debug and server-admin access to decals (players use the Decal Stencil):
 * {@code /decals decal <pos> <face> <decal|none> [colour] [rotation] [flip]} and
 * {@code /decals fill <from> <to> <face> <decal|none> [colour]}. A face is a direction or {@code front} (a panel's
 * front); a colour is a dye name or {@code RRGGBB} in hex.
 */
public final class DecalsCommand {
    private static final DynamicCommandExceptionType UNKNOWN_COLOUR =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown colour: " + id));
    private static final DynamicCommandExceptionType UNKNOWN_FACE =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown face: " + id));

    private DecalsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("decals")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("decal")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(faceArgument()
                                        .then(decalArgument()
                                                .executes(ctx -> setOne(ctx, "white", 0, false))
                                                .then(colourArgument()
                                                        .executes(ctx -> setOne(ctx, StringArgumentType.getString(ctx, "colour"), 0, false))
                                                        .then(Commands.argument("rotation", IntegerArgumentType.integer(0, 3))
                                                                .executes(ctx -> setOne(ctx, StringArgumentType.getString(ctx, "colour"),
                                                                        IntegerArgumentType.getInteger(ctx, "rotation"), false))
                                                                .then(Commands.argument("flip", BoolArgumentType.bool())
                                                                        .executes(ctx -> setOne(ctx, StringArgumentType.getString(ctx, "colour"),
                                                                                IntegerArgumentType.getInteger(ctx, "rotation"),
                                                                                BoolArgumentType.getBool(ctx, "flip"))))))))))
                .then(Commands.literal("fill")
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .then(faceArgument()
                                                .then(decalArgument()
                                                        .executes(ctx -> fill(ctx, "white"))
                                                        .then(colourArgument()
                                                                .executes(ctx -> fill(ctx, StringArgumentType.getString(ctx, "colour"))))))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> faceArgument() {
        return Commands.argument("face", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Stream.concat(Stream.of("front"), Arrays.stream(Direction.values()).map(Direction::getName)), builder));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> decalArgument() {
        return Commands.argument("decal", ResourceLocationArgument.id())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Stream.concat(Stream.of("none"), Decals.builtIn().stream().map(ResourceLocation::toString)), builder));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> colourArgument() {
        return Commands.argument("colour", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(DyeColor.values()).map(DyeColor::getSerializedName), builder));
    }

    private static int setOne(CommandContext<CommandSourceStack> ctx, String colour, int rotation, boolean flip)
            throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        DecalState decal = parse(ctx, colour, rotation, flip);
        if (!(ctx.getSource().getLevel().getBlockEntity(pos) instanceof DecalCopycatBlockEntity be)) {
            ctx.getSource().sendFailure(Component.literal("No decal block at " + pos.toShortString()));
            return 0;
        }
        Direction face = face(ctx, be);
        if (!be.acceptsDecalOn(face)) {
            ctx.getSource().sendFailure(Component.literal("A panel takes a decal on its front only"));
            return 0;
        }
        be.setDecal(face, decal);
        ctx.getSource().sendSuccess(() -> Component.literal("Decal set"), false);
        return 1;
    }

    private static int fill(CommandContext<CommandSourceStack> ctx, String colour) throws CommandSyntaxException {
        BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "to");
        DecalState decal = parse(ctx, colour, 0, false);
        ServerLevel level = ctx.getSource().getLevel();
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (level.getBlockEntity(pos) instanceof DecalCopycatBlockEntity be) {
                Direction face = face(ctx, be);
                if (be.acceptsDecalOn(face)) {
                    be.setDecal(face, decal);
                    count++;
                }
            }
        }
        int set = count;
        ctx.getSource().sendSuccess(() -> Component.literal("Decal set on " + set + " blocks"), false);
        return count;
    }

    private static Direction face(CommandContext<CommandSourceStack> ctx, DecalCopycatBlockEntity be) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "face");
        if (name.equals("front")) {
            for (Direction face : Direction.values()) {
                if (be.acceptsDecalOn(face)) {
                    return face;
                }
            }
        }
        Direction face = Direction.byName(name);
        if (face == null) {
            throw UNKNOWN_FACE.create(name);
        }
        return face;
    }

    @Nullable
    private static DecalState parse(CommandContext<CommandSourceStack> ctx, String colour, int rotation, boolean flip)
            throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "decal");
        if (id.getPath().equals("none")) {
            return null;
        }
        return new DecalState(id, parseColour(colour), rotation, flip);
    }

    private static int parseColour(String colour) throws CommandSyntaxException {
        DyeColor dye = DyeColor.byName(colour, null);
        if (dye != null) {
            return dye.getTextureDiffuseColor();
        }
        if (colour.length() == 6) {
            try {
                return Integer.parseInt(colour, 16);
            } catch (NumberFormatException ignored) {
                // falls through to the error
            }
        }
        throw UNKNOWN_COLOUR.create(colour);
    }
}
