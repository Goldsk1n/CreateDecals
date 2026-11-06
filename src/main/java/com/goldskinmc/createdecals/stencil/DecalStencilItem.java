package com.goldskinmc.createdecals.stencil;

import com.goldskinmc.createdecals.block.DecalCopycatBlockEntity;
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

/**
 * The Decal Stencil: used in the air it opens its screen (on the client, {@code client/StencilScreen}), where the decal,
 * colour, turns and flip are chosen; used on a face of a decal block it puts that decal there (or erases it); used
 * while sneaking it picks the face's decal up. Decals cost nothing to apply (plan Decision 8).
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
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof DecalCopycatBlockEntity be)) {
            return InteractionResult.PASS;
        }
        Direction face = context.getClickedFace();
        if (!be.acceptsDecalOn(face)) {
            message(player, "front_only");
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        StencilData data = data(stack);
        if (player != null && player.isShiftKeyDown()) {
            DecalState picked = be.getDecal(face);
            if (picked == null) {
                message(player, "nothing_to_pick");
            } else {
                stack.set(DecalsComponents.STENCIL.get(), StencilData.of(picked));
                message(player, "picked");
            }
            return InteractionResult.CONSUME;
        }
        if (data.erase()) {
            be.setDecal(face, null);
            return InteractionResult.CONSUME;
        }
        Optional<DecalState> decal = data.toDecal();
        if (decal.isEmpty()) {
            message(player, "no_decal");
            return InteractionResult.CONSUME;
        }
        be.setDecal(face, decal.get());
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
        } else if (data.decal().isPresent()) {
            lines.add(Component.translatable("createdecals.stencil.tooltip.decal", data.decal().get().getPath().replace('_', ' '),
                    String.format(Locale.ROOT, "#%06X", data.rgb())).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("createdecals.stencil.tooltip.empty").withStyle(ChatFormatting.GRAY));
        }
    }

    private static void message(Player player, String key) {
        if (player != null && !player.level().isClientSide()) {
            player.displayClientMessage(Component.translatable("createdecals.stencil." + key), true);
        }
    }
}
