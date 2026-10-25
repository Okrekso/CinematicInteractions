package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.Vec3;

/** Armor on the split campfire leg, using the same two segments as its skin. */
final class CampfireArmor {
    private static final LegMesh INNER = new LegMesh(0, 16, 0.5f, 32f);
    private static final LegMesh OUTER = new LegMesh(0, 16, 1f, 32f);

    private CampfireArmor() {}

    static void drawOnModel(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, boolean right,
            float knee, int light) {
        draw(player, buffers, right, (mesh, consumer, color) -> mesh.drawBentOnModel(pose, consumer, knee, light,
                OverlayTexture.NO_OVERLAY, color));
    }

    static void drawInScene(PoseStack pose, MultiBufferSource buffers, Vec3 camera, AbstractClientPlayer player, boolean right,
            Vec3 hip, Vec3 knee, Vec3 foot, Vec3 outward, int light) {
        draw(player, buffers, right, (mesh, consumer, color) -> mesh.drawBentInScene(pose, consumer, camera, hip, knee, foot,
                outward, light, color));
    }

    private static void draw(AbstractClientPlayer player, MultiBufferSource buffers, boolean right, Geometry geometry) {
        // Both armor pieces occupy the leg: leggings use the inner texture/mesh, boots the outer one.
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) continue;
            boolean inner = slot == EquipmentSlot.LEGS;
            LegMesh mesh = inner ? INNER : OUTER;
            for (ArmorMaterial.Layer layer : armor.getMaterial().value().layers()) {
                int color = layer.dyeable()
                        ? FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, DyedItemColor.LEATHER_COLOR)) : -1;
                ResourceLocation texture = layer.texture(inner);
                geometry.draw(mesh, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), color);
            }
            ArmorTrim trim = stack.get(DataComponents.TRIM);
            if (trim != null) {
                ResourceLocation trimTexture = inner ? trim.innerTexture(armor.getMaterial()) : trim.outerTexture(armor.getMaterial());
                VertexConsumer trimBuffer = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET)
                        .getSprite(trimTexture).wrap(buffers.getBuffer(Sheets.armorTrimsSheet(trim.pattern().value().decal())));
                geometry.draw(mesh, trimBuffer, -1);
            }
            if (stack.hasFoil()) geometry.draw(mesh, buffers.getBuffer(RenderType.armorEntityGlint()), -1);
        }
    }

    @FunctionalInterface
    private interface Geometry {
        void draw(LegMesh mesh, VertexConsumer consumer, int color);
    }
}
