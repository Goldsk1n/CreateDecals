package com.goldskinmc.createdecals.stencil;

import com.goldskinmc.createdecals.block.CreateCopycats;
import com.goldskinmc.createdecals.block.DecalEdits;
import com.goldskinmc.createdecals.block.DecalHolder;
import com.goldskinmc.createdecals.block.DecalTiles;
import com.goldskinmc.createdecals.block.DecalUndo;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.simibubi.create.AllSoundEvents;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Decal Stencil: used in the air it opens its screen (on the client, {@code client/StencilScreen}), where the decal,
 * colour, turns and flip are chosen; used on a face of a decal block it puts that decal there, a large one spreading
 * right and down from the clicked face over the faces next to it on the same plane (or erases the whole decal there);
 * used while sneaking it picks the face's decal up. Decals cost nothing to apply (plan Decision 8). Create's own Copycat
 * Panel takes a decal too, turning into a Decal Panel ({@code block/CreateCopycats}).
 */
public class DecalStencilItem extends Item {
    public DecalStencilItem(Properties properties) {
        super(properties);
    }

    public static StencilData data(ItemStack stack) {
        StencilData data = stack.get(DecalsComponents.STENCIL.get());
        return data != null ? data : StencilData.EMPTY;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        Direction face = DecalEdits.clickedFace(player, context.getClickedFace());
        // Our decal blocks and Copycats+'s copycats keep decals (DecalHolder); Create's own Copycat Panel and Step are
        // treated as ours without a decal: placing one converts them (CreateCopycats)
        BlockState state = level.getBlockState(context.getClickedPos());
        DecalHolder be = DecalHolder.at(level, context.getClickedPos());
        if (be == null && !CreateCopycats.isCreateCopycat(state)) {
            return InteractionResult.PASS;
        }
        if (be != null ? !be.acceptsDecalOn(face) : !CreateCopycats.convertible(state, face)) {
            message(player, "front_only");
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Every tile the click changes is kept as one step the player can undo (DecalUndo)
        return DecalUndo.record(player, level, () -> apply(context, level, player, face, be));
    }

    private InteractionResult apply(UseOnContext context, Level level, @Nullable Player player, Direction face,
            @Nullable DecalHolder be) {
        ItemStack stack = context.getItemInHand();
        StencilData data = data(stack);
        if (player != null && player.isShiftKeyDown()) {
            // The decal in the stencil's layer (or the other one, if that is bare); the stencil then goes on that layer
            Boolean front = be != null ? be.layerFor(face, data.front()) : null;
            if (front == null) {
                message(player, "nothing_to_pick");
            } else {
                stack.set(DecalsComponents.STENCIL.get(), StencilData.of(be.getDecal(face, front), front));
                message(player, front ? "picked_front" : "picked");
                AllSoundEvents.CLIPBOARD_CHECKMARK.playOnServer(level, context.getClickedPos());
            }
            return InteractionResult.CONSUME;
        }
        if (data.erase()) {
            // The decal in the stencil's layer goes (or the other one, if that is bare); nothing on Create's copycats
            Boolean front = be != null ? be.layerFor(face, data.front()) : null;
            if (front != null) {
                DecalTiles.changeWhole(level, context.getClickedPos(), face, front, tile -> null, DecalEdits.by(player, level, face, stack));
            }
            return InteractionResult.CONSUME;
        }
        if (data.writes()) {
            // A typed word: its first letter on the clicked face, the rest along the face's right, as turned; never mirrored
            int letters = DecalTiles.letters(data.text());
            int placed = DecalTiles.placeText(level, context.getClickedPos(), face, data.text(), data.rgb(), data.rotation(), false,
                    data.glow(), data.front(), DecalEdits.by(player, level, face, stack));
            if (placed < letters && player != null) {
                player.displayClientMessage(Component.translatable("createdecals.stencil.partly", placed, letters), true);
            }
            return InteractionResult.CONSUME;
        }
        Optional<DecalState> decal = data.toDecal();
        if (decal.isEmpty()) {
            message(player, "no_decal");
            return InteractionResult.CONSUME;
        }
        // The clicked face takes the decal's top-left tile; the rest go right and down from it on the same plane
        int tiles = decal.get().width() * decal.get().height();
        int placed = DecalTiles.place(level, context.getClickedPos(), face, decal.get(), data.front(), DecalEdits.by(player, level, face, stack));
        if (placed < tiles && player != null) {
            player.displayClientMessage(Component.translatable("createdecals.stencil.partly", placed, tiles), true);
        }
        return InteractionResult.CONSUME;
    }

    /** In the air: the client opens the screen (see client/StencilClientEvents); nothing happens on the server. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        StencilData data = data(stack);
        if (data.erase()) {
            lines.add(Component.translatable("createdecals.stencil.tooltip.erase").withStyle(ChatFormatting.GRAY));
            return;
        }
        String colour = String.format(Locale.ROOT, "#%06X", data.rgb());
        if (data.writes()) {
            lines.add(Component.translatable("createdecals.stencil.tooltip.text", data.text(), colour).withStyle(ChatFormatting.GRAY));
        } else if (data.decal().isPresent()) {
            lines.add(Component.translatable("createdecals.stencil.tooltip.decal", data.decal().get().getPath().replace('_', ' '),
                    colour).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("createdecals.stencil.tooltip.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (data.front()) {
            lines.add(Component.translatable("createdecals.stencil.tooltip.front").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (data.glow()) {
            lines.add(Component.translatable("createdecals.stencil.tooltip.glow").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void message(Player player, String key) {
        if (player != null && !player.level().isClientSide()) {
            player.displayClientMessage(Component.translatable("createdecals.stencil." + key), true);
        }
    }
}
