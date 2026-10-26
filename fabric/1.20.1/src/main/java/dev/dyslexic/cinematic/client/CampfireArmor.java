package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.phys.Vec3;

/** Renders leggings and boots along the split campfire leg, including dye, trim and glint. */
final class CampfireArmor {
    private static final LegMesh INNER = new LegMesh(0, 16, 0.5f, 32f, 0.25f);
    private static final LegMesh OUTER = new LegMesh(0, 16, 1f, 32f, 0.25f);

    private CampfireArmor() {}

    static void drawOnModel(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, float knee, int light) {
        draw(player, buffers, (mesh, consumer, red, green, blue, boots) -> {
            if (boots) mesh.drawShinOnModel(pose, consumer, knee, light, red, green, blue);
            else mesh.drawBentOnModel(pose, consumer, knee, light, red, green, blue);
        });
    }

    static void drawInScene(PoseStack pose, MultiBufferSource buffers, Vec3 camera, AbstractClientPlayer player,
            boolean right, Vec3 hip, Vec3 knee, Vec3 foot, Vec3 outward, int light) {
        draw(player, buffers, (mesh, consumer, red, green, blue, boots) -> {
            if (boots) mesh.drawShinInScene(pose, consumer, camera, knee, foot, outward, light, red, green, blue);
            else mesh.drawBentInScene(pose, consumer, camera, hip, knee, foot, outward, light, red, green, blue);
        });
    }

    private static void draw(AbstractClientPlayer player, MultiBufferSource buffers, Geometry geometry) {
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot) continue;
            boolean inner = slot == EquipmentSlot.LEGS;
            LegMesh mesh = inner ? INNER : OUTER;
            String suffix = armor instanceof DyeableLeatherItem ? "overlay" : null;
            // the vanilla armor texture path (HumanoidArmorLayer.getArmorLocation); Fabric has no per-item texture hook here
            String base = "textures/models/armor/" + armor.getMaterial().getName() + "_layer_" + (inner ? 2 : 1) + ".png";
            ResourceLocation texture = new ResourceLocation(base);
            int color = armor instanceof DyeableLeatherItem dyeable ? dyeable.getColor(stack) : 0xffffff;
            geometry.draw(mesh, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)),
                    (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, !inner);
            if (suffix != null) {
                String overlay = "textures/models/armor/" + armor.getMaterial().getName() + "_layer_" + (inner ? 2 : 1) + "_overlay.png";
                ResourceLocation over = new ResourceLocation(overlay);
                geometry.draw(mesh, buffers.getBuffer(RenderType.armorCutoutNoCull(over)), 1f, 1f, 1f, !inner);
            }
            ArmorTrim.getTrim(player.level().registryAccess(), stack).ifPresent(trim -> {
                ResourceLocation trimTexture = inner ? trim.innerTexture(armor.getMaterial()) : trim.outerTexture(armor.getMaterial());
                VertexConsumer trimBuffer = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET)
                        .getSprite(trimTexture).wrap(buffers.getBuffer(Sheets.armorTrimsSheet()));
                geometry.draw(mesh, trimBuffer, 1f, 1f, 1f, !inner);
            });
            if (stack.hasFoil()) {
                geometry.draw(mesh, buffers.getBuffer(RenderType.armorEntityGlint()), 1f, 1f, 1f, !inner);
            }
        }
    }

    @FunctionalInterface
    private interface Geometry {
        void draw(LegMesh mesh, VertexConsumer consumer, float red, float green, float blue, boolean boots);
    }
}
