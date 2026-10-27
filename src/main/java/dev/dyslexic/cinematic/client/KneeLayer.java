package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
//? if neoforge {
import com.mojang.math.Axis;
//?}
import dev.dyslexic.cinematic.station.StationKind;
//? if neoforge {
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
//?}
import net.minecraft.client.model.PlayerModel;
//? if neoforge {
import net.minecraft.client.model.geom.ModelPart;
//?}
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
//? if neoforge {
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
//?}
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
//? if neoforge {
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
//?} else if forge {
/*import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
*///?}

/**
 * The raised campfire leg, split at its knee, as others see it: the thigh drawn up and the shin back down to the ground.
 * The straight leg (with its pants and armor) is hidden meanwhile ({@code HumanoidModelMixin}, {@code CampfireArmorMixin}).
 */
public final class KneeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /**
     * The thigh's pitch (well above level, the knee high) and the shin's bend at the knee: the shin leans
     * forward from the knee and the foot comes down onto the ground in front of the seat.
     */
    public static final float THIGH = -2.1f;
    public static final float KNEE = 1.5f;
    //? if neoforge {
    public static boolean bends = true;
    //?} else if fabric {
    /*/^* For the walkthrough's before/after: the straight vanilla leg. ^/
    public static boolean bends = true;
    *///?}

    //? if neoforge {
    private final ModelPart skinThigh = part(0, 16, 0f, 64f);
    private final ModelPart skinShin = part(0, 22, 0f, 64f);
    private final ModelPart pantsThigh = part(0, 32, 0.25f, 64f);
    private final ModelPart pantsShin = part(0, 38, 0.25f, 64f);
    /** The leggings (the inner armor model) and the boots (the outer one), bent alike. */
    private final ModelPart leggings = armorLeg(0.5f);
    private final ModelPart boots = armorLeg(1f);

    //?}
    public KneeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    //? if neoforge {
    public static boolean sitting(int id) {
        return bends && ClientPayloads.atStation(id) == StationKind.CAMPFIRE;
    //?} else if fabric {
    /*public static boolean sitting(AbstractClientPlayer player) {
        return bends && ClientPayloads.atStation(player.getId()) == StationKind.CAMPFIRE;
    *///?} else if forge {
    /*public static void addTo(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new KneeLayer(renderer));
        }
    }

    public static boolean sitting(AbstractClientPlayer player) {
        return ClientPayloads.atStation(player.getId()) == StationKind.CAMPFIRE;
    *///?}
    }

    @Override
    //? if neoforge {
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!sitting(player.getId())) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0f);
    //?} else if fabric {
    /*public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!sitting(player)) {
            return;
        }
    *///?} else if forge {
    /*public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!sitting(player)) return;
    *///?}
        pose.pushPose();
        // the hidden leg's own pivot and turn, as the model would have drawn it
        getParentModel().rightLeg.translateAndRotate(pose);
        if (!player.isInvisible()) {
            //? if neoforge {
            var texture = player.getSkin().texture();
            renderParts(pose, buffers.getBuffer(RenderType.entitySolid(texture)), light, overlay, skinThigh, skinShin);
            if (player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG)) {
                renderParts(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)), light, overlay, pantsThigh, pantsShin);
            }
            //?} else if !neoforge {
            /*LegMesh.drawOnModel(pose, buffers, player, true, KNEE, light, LivingEntityRenderer.getOverlayCoords(player, 0));
            *///?}
        }
        //? if neoforge {
        renderArmor(pose, buffers, light, player, EquipmentSlot.LEGS, leggings);
        renderArmor(pose, buffers, light, player, EquipmentSlot.FEET, boots);
        //?} else if fabric && >=1.21 {
        /*CampfireArmor.drawOnModel(pose, buffers, player, true, KNEE, light);
        *///?} else if <1.21 {
        /*CampfireArmor.drawOnModel(pose, buffers, player, KNEE, light);
        *///?}
        pose.popPose();
    //? if neoforge {
    }

    private static void renderParts(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, int light, int overlay, ModelPart thigh, ModelPart shin) {
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

    /** The armor of the bent leg, drawn the way Minecraft's armor layer draws it: its layers, its trim and its glint. */
    private static void renderArmor(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, EquipmentSlot slot, ModelPart leg) {
        ItemStack stack = player.getItemBySlot(slot);
        if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) {
            return;
        }
        boolean inner = slot == EquipmentSlot.LEGS;
        ArmorMaterial material = armor.getMaterial().value();
        IClientItemExtensions extensions = IClientItemExtensions.of(stack);
        int fallbackColor = extensions.getDefaultDyeColor(stack);
        for (int i = 0; i < material.layers().size(); i++) {
            ArmorMaterial.Layer layer = material.layers().get(i);
            int color = extensions.getArmorLayerTintColor(stack, player, layer, i, fallbackColor);
            if (color != 0) {
                var texture = ClientHooks.getArmorTexture(player, stack, layer, inner, slot);
                leg.render(pose, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY, color);
            }
        }
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim != null) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET)
                    .getSprite(inner ? trim.innerTexture(armor.getMaterial()) : trim.outerTexture(armor.getMaterial()));
            leg.render(pose, sprite.wrap(buffers.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal()))), light, OverlayTexture.NO_OVERLAY);
        }
        if (stack.hasFoil()) {
            leg.render(pose, buffers.getBuffer(RenderType.armorEntityGlint()), light, OverlayTexture.NO_OVERLAY);
        }
    }

    private static ModelPart armorLeg(float grow) {
        ModelPart upper = part(0, 16, grow, 32f);
        ModelPart lower = part(0, 22, grow - 0.02f, 32f);
        upper.yScale = 8f / 6f;
        lower.y = 6f;
        lower.xRot = KNEE;
        return new ModelPart(List.of(), Map.of("thigh", upper, "shin", lower));
    }

    private static ModelPart part(int u, int v, float grow, float textureHeight) {
        ModelPart.Cube cube = new ModelPart.Cube(u, v, -2f, 0f, -2f, 4f, 6f, 4f,
                grow, grow, grow, false, 64f, textureHeight, EnumSet.allOf(Direction.class));
        return new ModelPart(List.of(cube), Map.of());
    }

    public static void addTo(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new KneeLayer(renderer));
            }
        }
    //?}
    }
}
