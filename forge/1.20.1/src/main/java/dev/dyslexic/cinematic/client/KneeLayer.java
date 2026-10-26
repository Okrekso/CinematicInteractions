package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** A raised knee made of a thigh and shin instead of the straight vanilla leg. */
public final class KneeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /**
     * The thigh's pitch (well above level, the knee high) and the shin's bend at the knee: the shin leans
     * forward from the knee and the foot comes down onto the ground in front of the seat.
     */
    public static final float THIGH = -2.1f;
    public static final float KNEE = 1.5f;

    public KneeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void addTo(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new KneeLayer(renderer));
        }
    }

    public static boolean sitting(AbstractClientPlayer player) {
        return ClientPayloads.atStation(player.getId()) == StationKind.CAMPFIRE;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!sitting(player)) return;
        pose.pushPose();
        getParentModel().rightLeg.translateAndRotate(pose);
        if (!player.isInvisible()) {
            LegMesh.drawOnModel(pose, buffers, player, true, KNEE, light, LivingEntityRenderer.getOverlayCoords(player, 0));
        }
        CampfireArmor.drawOnModel(pose, buffers, player, KNEE, light);
        pose.popPose();
    }
}
