package com.goldskinmc.createdecals.stencil;

import com.goldskinmc.createdecals.block.CreatePanels;
import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
import com.goldskinmc.createdecals.block.DecalTiles;
import com.goldskinmc.createdecals.decal.DecalState;
import com.goldskinmc.createdecals.registry.DecalsComponents;
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

/**
 * The Decal Stencil: used in the air it opens its screen (on the client, {@code client/StencilScreen}), where the decal,
 * colour, turns and flip are chosen; used on a face of a decal block it puts that decal there, a large one spreading
 * right and down from the clicked face over the faces next to it on the same plane (or erases the whole decal there);
 * used while sneaking it picks the face's decal up. Decals cost nothing to apply (plan Decision 8). Create's own Copycat
 * Panel takes a decal too, turning into a Decal Panel ({@code block/CreatePanels}).
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
        Direction face = context.getClickedFace();
        // Create's own Copycat Panel is treated as a Decal Panel without a decal: placing one converts it (CreatePanels)
        BlockState state = level.getBlockState(context.getClickedPos());
        DecalCopycatBlockEntity be = level.getBlockEntity(context.getClickedPos()) instanceof DecalCopycatBlockEntity ours ? ours : null;
        if (be == null && !CreatePanels.isCreatePanel(state)) {
            return InteractionResult.PASS;
        }
        if (be != null ? !be.acceptsDecalOn(face) : !CreatePanels.convertible(state, face)) {
            message(player, "front_only");
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        StencilData data = data(stack);
        if (player != null && player.isShiftKeyDown()) {
            // The decal seen on top, and the stencil then goes on that layer
            Boolean front = be != null ? be.topLayer(face) : null;
            if (front == null) {
                message(player, "nothing_to_pick");
            } else {
                stack.set(DecalsComponents.STENCIL.get(), StencilData.of(be.getDecal(face, front), front));
                message(player, front ? "picked_front" : "picked");
            }
            return InteractionResult.CONSUME;
        }
        if (data.erase()) {
            // The decal seen on top goes, so a front decal comes off before the one under it; nothing on Create's panel
            DecalTiles.changeWhole(level, context.getClickedPos(), face, tile -> null);
            return InteractionResult.CONSUME;
        }
        if (data.writes()) {
            // A typed word: its first letter on the clicked face, the rest along the face's right (as turned and flipped)
            int letters = DecalTiles.letters(data.text());
            int placed = DecalTiles.placeText(level, context.getClickedPos(), face, data.text(), data.rgb(), data.rotation(), data.flip(),
                    data.glow(), data.front());
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
        int placed = DecalTiles.place(level, context.getClickedPos(), face, decal.get(), data.front());
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
