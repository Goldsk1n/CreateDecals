package com.goldskinmc.livery.command;

import com.goldskinmc.livery.decal.Decal;
import com.goldskinmc.livery.decal.DecalState;
import com.goldskinmc.livery.panel.DecalPanelBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/**
 * Prototype stand-in for a decal picker: {@code /livery decal <pos> <decal|none> [colour] [rotation] [flip]} and
 * {@code /livery fill <from> <to> <decal|none> [colour]}. A colour is a dye name or {@code RRGGBB} in hex.
 */
public final class LiveryCommand {
    private static final DynamicCommandExceptionType UNKNOWN_DECAL =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown decal: " + id));
    private static final DynamicCommandExceptionType UNKNOWN_COLOUR =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown colour: " + id));

    private LiveryCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("livery")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("decal")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
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
                                                                        BoolArgumentType.getBool(ctx, "flip")))))))))
                .then(Commands.literal("fill")
                        .then(Commands.argument("from", BlockPosArgument.blockPos())
                                .then(Commands.argument("to", BlockPosArgument.blockPos())
                                        .then(decalArgument()
                                                .executes(ctx -> fill(ctx, "white"))
                                                .then(colourArgument()
                                                        .executes(ctx -> fill(ctx, StringArgumentType.getString(ctx, "colour")))))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> decalArgument() {
        return Commands.argument("decal", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Stream.concat(Stream.of("none"), Arrays.stream(Decal.values()).map(Decal::id)), builder));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> colourArgument() {
        return Commands.argument("colour", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(DyeColor.values()).map(DyeColor::getSerializedName), builder));
    }

    private static int setOne(CommandContext<CommandSourceStack> ctx, String colour, int rotation, boolean flip)
            throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        DecalState decal = parse(StringArgumentType.getString(ctx, "decal"), colour, rotation, flip);
        if (!(ctx.getSource().getLevel().getBlockEntity(pos) instanceof DecalPanelBlockEntity be)) {
            ctx.getSource().sendFailure(Component.literal("No decal panel at " + pos.toShortString()));
            return 0;
        }
        be.setDecal(decal);
        ctx.getSource().sendSuccess(() -> Component.literal("Decal set"), false);
        return 1;
    }

    private static int fill(CommandContext<CommandSourceStack> ctx, String colour) throws CommandSyntaxException {
        BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "to");
        DecalState decal = parse(StringArgumentType.getString(ctx, "decal"), colour, 0, false);
        ServerLevel level = ctx.getSource().getLevel();
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
            if (level.getBlockEntity(pos) instanceof DecalPanelBlockEntity be) {
                be.setDecal(decal);
                count++;
            }
        }
        int set = count;
        ctx.getSource().sendSuccess(() -> Component.literal("Decal set on " + set + " panels"), false);
        return count;
    }

    @Nullable
    private static DecalState parse(String decalId, String colour, int rotation, boolean flip) throws CommandSyntaxException {
        if (decalId.equals("none")) {
            return null;
        }
        Decal decal = Decal.byId(decalId);
        if (decal == null) {
            throw UNKNOWN_DECAL.create(decalId);
        }
        return new DecalState(decal, parseColour(colour), rotation, flip);
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
