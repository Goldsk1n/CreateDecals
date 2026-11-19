package com.goldskinmc.createdecals.contraption;

import com.goldskinmc.createdecals.client.ClientSafety;
import com.goldskinmc.createdecals.client.ContraptionGlow;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * The decal blocks on a Create contraption or train. They do nothing while moving; the behaviour only lets glowing decals
 * be drawn again at full light ({@link ContraptionGlow}). Create meshes a contraption's blocks into a buffer whose vertex
 * writing Sodium replaces with a fast path that keeps the light the block renderer worked out and drops each quad's own
 * baked light, so without this a glowing decal on a moving contraption looked like any other under Sodium.
 */
public class DecalMovement implements MovementBehaviour {
    /** Contraption Controls have nothing to switch off here. */
    @Override
    @Nullable
    public ItemStack canBeDisabledVia(MovementContext context) {
        return null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices,
            MultiBufferSource buffer) {
        ClientSafety.run(ClientSafety.GLOW, () -> ContraptionGlow.render(context, renderWorld, matrices, buffer));
    }
}
