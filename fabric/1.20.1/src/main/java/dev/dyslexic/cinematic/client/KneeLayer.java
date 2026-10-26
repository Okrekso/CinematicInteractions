package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * The knee drawn up by a player sitting at a campfire, as others see it (and the player too, looking round from
 * behind): the vanilla leg is one straight piece, so raised it pointed at the sky. It is hidden then
 * ({@code HumanoidModelMixin}) and drawn here in two, thigh up to the knee and shin down to the ground.
 */
public final class KneeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** The raised thigh, above the horizontal (the model's leg rotation), and the shin's turn at the knee from it. */
    public static final float THIGH = -2.1f;
    public static final float KNEE = 1.5f;
    /** For the walkthrough's before/after: the straight vanilla leg. */
    public static boolean bends = true;

    public KneeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static boolean sitting(AbstractClientPlayer player) {
        return bends && ClientPayloads.atStation(player.getId()) == StationKind.CAMPFIRE;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!sitting(player)) {
            return;
        }
        pose.pushPose();
        // the hidden leg's own pivot and turn, as the model would have drawn it
        getParentModel().rightLeg.translateAndRotate(pose);
        if (!player.isInvisible()) {
            LegMesh.drawOnModel(pose, buffers, player, true, KNEE, light, LivingEntityRenderer.getOverlayCoords(player, 0));
        }
        CampfireArmor.drawOnModel(pose, buffers, player, KNEE, light);
        pose.popPose();
    }
}
