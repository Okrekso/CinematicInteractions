package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.AnvilStation;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.ClientPayloads;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.KneeLayer;
import dev.dyslexic.cinematic.client.StonecutterStation;
import dev.dyslexic.cinematic.client.ShelfBooks;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Other players see someone at a station in the matching pose: hands resting on a table, a blast furnace or a smithing
 * table, crouched at a furnace reaching in, both hands raised over an enchanting table. A move plays on the same
 * timeline as in the scene: the hammer (in the hand, {@code PropLayer}) swung down onto the anvil blow by blow, the blade
 * drawn to and fro over the grindstone, the stone pushed through the saw and the piece caught.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Shadow
    @Final
    public ModelPart rightArm;

    @Shadow
    @Final
    public ModelPart leftArm;

    @Shadow
    @Final
    public ModelPart rightLeg;

    @Shadow
    @Final
    public ModelPart leftLeg;

    @Shadow
    @Final
    public ModelPart body;

    @Shadow
    @Final
    public ModelPart head;

    /** How far the body leans back from the hips while sitting by a campfire, on the left hand. */
    @Unique
    private static final float CINEMATIC$LEAN = 0.45f;

    /** The body and head were moved back by {@link #cinematic$leanBack} (Minecraft resets neither of their z itself). */
    @Unique
    private boolean cinematic$leaned;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void cinematic$stationPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch, CallbackInfo ci) {
        if (cinematic$leaned) {
            // (the model is shared by every player it draws: the next one sits up straight again)
            body.z = 0f;
            head.z = 0f;
            cinematic$leaned = false;
        }
        if (!(entity instanceof Player)) {
            return;
        }
        StationKind kind = ClientPayloads.atStation(entity.getId());
        if (kind == null) {
            return;
        }
        float t = ageInTicks * 0.09f + entity.getId();
        double move = ClientMenus.sinceMove(entity.getId());
        switch (kind) {
            case ANVIL -> {
                // the left hand holds the work; the right swings the hammer, turned in towards the work
                leftArm.xRot = -1.05f + Mth.sin(t + 2.1f) * 0.04f;
                leftArm.yRot = 0.28f;
                leftArm.zRot = -0.06f;
                boolean forging = move < AnvilStation.FORGE_LENGTH;
                rightArm.xRot = forging ? AnvilStation.armPitch(move) : -0.98f + Mth.sin(t) * 0.05f;
                rightArm.yRot = forging ? -0.35f : -0.2f;
                rightArm.zRot = 0.08f;
            }
            case CAMPFIRE -> {
                // sitting on the ground by the fire, right knee drawn up with the hand on it, leaning on the left hand; the
                // raised leg is drawn bent at the knee by KneeLayer (one straight piece would point at the sky)
                boolean bent = dev.dyslexic.cinematic.client.KneeLayer.bends;
                rightLeg.xRot = bent ? dev.dyslexic.cinematic.client.KneeLayer.THIGH : -2.0f;
                rightLeg.yRot = 0.12f;
                rightLeg.zRot = 0.05f;
                leftLeg.xRot = -1.45f;
                leftLeg.yRot = -0.22f;
                leftLeg.zRot = 0f;
                if (KneeLayer.sitting(entity.getId())) {
                    // KneeLayer draws this leg bent at the knee instead (the renderer shows every part again next frame)
                    rightLeg.xRot = KneeLayer.THIGH;
                    rightLeg.visible = false;
                    if ((Object) this instanceof PlayerModel<?> player) {
                        player.rightPants.visible = false;
                    }
                    cinematic$leanBack();
                    // leaning back, the right forearm lies over the raised knee and the left hand is on the ground behind
                    rightArm.xRot = -1.5f + Mth.sin(t * 0.6f) * 0.03f;
                    rightArm.yRot = -0.3f;
                    rightArm.zRot = 0.05f;
                    leftArm.xRot = 0.4f;
                    leftArm.yRot = 0f;
                    leftArm.zRot = -0.25f;
                } else {
                    rightArm.xRot = -1.05f + Mth.sin(t * 0.6f) * 0.03f;
                    rightArm.yRot = -0.1f;
                    rightArm.zRot = 0.05f;
                    leftArm.xRot = 0.35f;
                    leftArm.yRot = 0f;
                    leftArm.zRot = -0.32f;
                }
            }
            case LOOM -> {
                // throwing the shuttle across the warp from hand to hand
                float throwing = Mth.sin(t * 2.2f) * 0.2f;
                rightArm.xRot = -1.0f + Mth.sin(t * 1.1f) * 0.04f;
                rightArm.yRot = -0.35f + throwing;
                rightArm.zRot = 0.05f;
                leftArm.xRot = -1.0f + Mth.sin(t * 1.1f + 1.3f) * 0.04f;
                leftArm.yRot = 0.35f + throwing;
                leftArm.zRot = -0.05f;
            }
            case GRINDSTONE -> {
                // both hands forward and low, holding a blade to the wheel and drawing it across while it grinds
                float stroke = move < GrindstoneStation.GRIND_LENGTH ? GrindstoneStation.armSway(move) : 0f;
                rightArm.xRot = -1.15f + Mth.sin(t * 1.1f) * 0.03f;
                rightArm.yRot = -0.32f + stroke;
                rightArm.zRot = 0.05f;
                leftArm.xRot = -1.2f + Mth.sin(t * 1.1f + 1.3f) * 0.03f;
                leftArm.yRot = 0.3f + stroke;
                leftArm.zRot = -0.05f;
            }
            case STONECUTTER -> {
                // leaning over the saw: the left hand pushes the stone through, the right waits for the piece past the blade
                float[] turns = move < StonecutterStation.CUT_LENGTH ? StonecutterStation.armTurns(move) : new float[]{-0.2f, -0.12f};
                leftArm.xRot = -1.0f + Mth.sin(t + 2.1f) * 0.04f;
                leftArm.yRot = turns[0];
                leftArm.zRot = -0.06f;
                rightArm.xRot = -0.98f + Mth.sin(t) * 0.05f;
                rightArm.yRot = turns[1];
                rightArm.zRot = 0.06f;
            }
            case FURNACE, SMOKER, BREWING -> {
                // crouched at the front: one hand reaching into the openings, the other steadying
                rightArm.xRot = -1.32f + Mth.sin(t * 1.3f) * 0.12f;
                rightArm.yRot = -0.12f + Mth.sin(t * 0.8f) * 0.08f;
                rightArm.zRot = 0.04f;
                leftArm.xRot = -1.6f + Mth.sin(t + 2.1f) * 0.03f;
                leftArm.yRot = 0.32f;
                leftArm.zRot = -0.05f;
            }
            case CHISELED_BOOKSHELF -> {
                if (ShelfBooks.reading(entity.getId())) {
                    // a book held open in both hands in front of the chest
                    rightArm.xRot = -1.05f + Mth.sin(t * 0.6f) * 0.02f;
                    rightArm.yRot = -0.42f;
                    rightArm.zRot = 0.05f;
                    leftArm.xRot = -1.05f + Mth.sin(t * 0.6f + 1.4f) * 0.02f;
                    leftArm.yRot = 0.42f;
                    leftArm.zRot = -0.05f;
                } else {
                    // crouched at the shelf: one hand along the books, the other on the knee
                    rightArm.xRot = -1.4f + Mth.sin(t * 0.9f) * 0.1f;
                    rightArm.yRot = -0.1f + Mth.sin(t * 0.55f) * 0.18f;
                    rightArm.zRot = 0.04f;
                    leftArm.xRot = -0.55f;
                    leftArm.yRot = 0.1f;
                    leftArm.zRot = -0.1f;
                }
            }
            case ENCHANTING, BEACON -> {
                // both hands raised over the table (or towards the beacon), palms to the floating item
                rightArm.xRot = -1.75f + Mth.sin(t * 0.7f) * 0.06f;
                rightArm.yRot = -0.32f + Mth.sin(t * 0.5f) * 0.05f;
                rightArm.zRot = 0.18f;
                leftArm.xRot = -1.75f + Mth.sin(t * 0.7f + 1.4f) * 0.06f;
                leftArm.yRot = 0.32f;
                leftArm.zRot = -0.18f;
            }
            default -> {
                // leaning over a table or a blast furnace, hands resting on top
                rightArm.xRot = -0.98f + Mth.sin(t) * 0.07f;
                rightArm.yRot = -0.16f + Mth.sin(t * 0.7f) * 0.05f;
                rightArm.zRot = 0.06f;
                leftArm.xRot = -0.98f + Mth.sin(t + 2.1f) * 0.05f;
                leftArm.yRot = 0.16f;
                leftArm.zRot = -0.06f;
            }
        }
    }

    /**
     * Tilts the body back about the hips (the legs stay where they are): the torso turns, and the head and the
     * shoulders move with its top, 12 and 10 pixels above the hips.
     */
    @Unique
    private void cinematic$leanBack() {
        float cos = Mth.cos(CINEMATIC$LEAN), sin = Mth.sin(CINEMATIC$LEAN);
        body.xRot = -CINEMATIC$LEAN;
        body.y = 12f - 12f * cos;
        body.z = 12f * sin;
        head.y = body.y;
        head.z = body.z;
        rightArm.y = 12f - 10f * cos;
        rightArm.z = 10f * sin;
        leftArm.y = rightArm.y;
        leftArm.z = rightArm.z;
        cinematic$leaned = true;
    }
}
