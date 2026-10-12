package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;


/**
 * What a player at a station holds, as others see it: the smith's hammer in the right hand while they forge (the arm
 * swings it, {@code HumanoidModelMixin}, on the scene's own timeline). Whatever they really hold in that hand is not drawn
 * meanwhile ({@code ItemInHandLayerMixin}).
 */
public final class PropLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    public PropLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent) {
        super(parent);
    }

    /** How much of the hammer this player has in hand: 0 none, 1 all of it (it comes off the belt and goes back). */
    public static double hammer(Entity entity) {
        return hammer(entity.getId());
    }

    public static double hammer(int id) {
        return ClientPayloads.atStation(id) == StationKind.ANVIL ? AnvilStation.hammerShown(ClientMenus.sinceMove(id)) : 0;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, PlayerRenderState player, float netHeadYaw, float headPitch) {
        double shown = hammer(player.id);
        if (shown <= 0.001) {
            return;
        }
        pose.pushPose();
        getParentModel().translateToHand(HumanoidArm.RIGHT, pose);
        HammerModel.renderInArm(pose, buffers, shown, light);
        pose.popPose();
    }

}
