package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/**
 * The raised campfire leg, split at its knee, as others see it: the thigh drawn up and the shin down to the ground in
 * front of the seat. The straight leg (its pants hang from it) and its armor are hidden meanwhile
 * ({@code HumanoidModelMixin}, {@code CampfireArmorMixin}); the armor is drawn here by Minecraft's equipment renderer.
 */
public final class KneeLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    /**
     * The thigh's pitch (well above level, the knee high) and the shin's bend at the knee: the shin leans
     * forward from the knee and the foot comes down onto the ground in front of the seat.
     */
    public static final float THIGH = -2.1f;
    public static final float KNEE = 1.5f;
    public static boolean bends = true;

    private final EquipmentLayerRenderer equipment;
    private final ModelPart skinThigh = part(0, 16, 0f, 64f);
    private final ModelPart skinShin = part(0, 22, 0f, 64f);
    private final ModelPart pantsThigh = part(0, 32, 0.25f, 64f);
    private final ModelPart pantsShin = part(0, 38, 0.25f, 64f);
    /** The leggings (the inner armor model) and the boots (the outer one), bent alike. */
    private final Model leggings = armorModel(0.5f);
    private final Model boots = armorModel(1f);

    public KneeLayer(RenderLayerParent<PlayerRenderState, PlayerModel> parent, EquipmentLayerRenderer equipment) {
        super(parent);
        this.equipment = equipment;
    }

    public static boolean sitting(int id) {
        return bends && ClientPayloads.atStation(id) == StationKind.CAMPFIRE;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, PlayerRenderState player, float netHeadYaw, float headPitch) {
        if (!sitting(player.id)) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0f);
        pose.pushPose();
        getParentModel().rightLeg.translateAndRotate(pose);
        if (!player.isInvisible) {
            var texture = player.skin.texture();
            renderParts(pose, buffers.getBuffer(RenderType.entitySolid(texture)), light, overlay, skinThigh, skinShin);
            if (player.showRightPants) {
                renderParts(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)), light, overlay, pantsThigh, pantsShin);
            }
        }
        renderArmor(pose, buffers, light, player.legsEquipment, EquipmentSlot.LEGS, leggings, EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS);
        renderArmor(pose, buffers, light, player.feetEquipment, EquipmentSlot.FEET, boots, EquipmentClientInfo.LayerType.HUMANOID);
        pose.popPose();
    }

    private static void renderParts(PoseStack pose, VertexConsumer buffer, int light, int overlay, ModelPart thigh, ModelPart shin) {
        pose.pushPose();
        pose.scale(1f, 8f / 6f, 1f);
        thigh.render(pose, buffer, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, 6f / 16f, 0);
        pose.mulPose(Axis.XP.rotation(KNEE));
        shin.render(pose, buffer, light, overlay);
        pose.popPose();
    }

    private void renderArmor(PoseStack pose, MultiBufferSource buffers, int light, ItemStack stack, EquipmentSlot slot, Model model,
            EquipmentClientInfo.LayerType layer) {
        Equippable gear = stack.get(DataComponents.EQUIPPABLE);
        if (gear == null || gear.slot() != slot || gear.assetId().isEmpty()) {
            return;
        }
        equipment.renderLayers(layer, gear.assetId().get(), model, stack, pose, buffers, light);
    }

    private static Model armorModel(float grow) {
        ModelPart upper = part(0, 16, grow, 32f);
        ModelPart lower = part(0, 22, grow - 0.02f, 32f);
        upper.yScale = 8f / 6f;
        lower.y = 6f;
        lower.xRot = KNEE;
        ModelPart root = new ModelPart(List.of(), Map.of("thigh", upper, "shin", lower));
        return new Model(root, RenderType::entitySolid) {};
    }

    private static ModelPart part(int u, int v, float grow, float textureHeight) {
        ModelPart.Cube cube = new ModelPart.Cube(u, v, -2f, 0f, -2f, 4f, 6f, 4f,
                grow, grow, grow, false, 64f, textureHeight, EnumSet.allOf(Direction.class));
        return new ModelPart(List.of(cube), Map.of());
    }
}
