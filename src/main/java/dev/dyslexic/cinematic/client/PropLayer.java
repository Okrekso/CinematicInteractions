package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
//? if neoforge {
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
//?} else if forge {
/*import net.minecraft.client.renderer.entity.player.PlayerRenderer;
*///?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
//? if neoforge {
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//?} else if forge {
/*import net.minecraftforge.client.event.EntityRenderersEvent;
*///?}

/**
 * What a player at a station holds, as others see it: the smith's hammer in the right hand while they forge (the arm
 * swings it, {@code HumanoidModelMixin}, on the scene's own timeline). Whatever they really hold in that hand is not drawn
 * meanwhile ({@code ItemInHandLayerMixin}).
 */
public final class PropLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public PropLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    /** How much of the hammer this player has in hand: 0 none, 1 all of it (it comes off the belt and goes back). */
    public static double hammer(Entity entity) {
        return ClientPayloads.atStation(entity.getId()) == StationKind.ANVIL ? AnvilStation.hammerShown(ClientMenus.sinceMove(entity.getId())) : 0;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        double shown = hammer(player);
        if (shown <= 0.001) {
            return;
        }
        pose.pushPose();
        getParentModel().translateToHand(HumanoidArm.RIGHT, pose);
        HammerModel.renderInArm(pose, buffers, shown, light);
        pose.popPose();
    //? if neoforge {
    }

    public static void addTo(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new PropLayer(renderer));
            }
        }
    //?} else if forge {
    /*}

    public static void addTo(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new PropLayer(renderer));
            }
        }
    *///?}
    }
}
