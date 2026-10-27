package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the workbench scene place the camera after vanilla has set it up at the player's eyes. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    //? if forge {
    /*@Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    @Final
    private Vector3f forwards;

    @Shadow
    @Final
    private Vector3f up;

    @Shadow
    @Final
    private Vector3f left;

    *///?}
    @Inject(method = "setup", at = @At("TAIL"))
    private void cinematic$scene(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick,
            CallbackInfo ci) {
        Camera self = (Camera) (Object) this;
        // the scene turns the player's body once a tick, and the camera sets off from (and comes back to) the eyes: their
        // rotation is followed between ticks, as the game shows anyone else turning (the player's own view is not, since
        // the mouse turns it every frame and keeps the rotation of the last tick in step)
        float yaw = self.getYRot();
        float pitch = self.getXRot();
        if (Scene.active() && entity instanceof LocalPlayer player && !player.isPassenger()) {
            yaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
            pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        }
        CameraRig.Pose pose = CameraRig.override(self.getPosition(), yaw, pitch);
        if (pose != null) {
            setRotation(pose.yaw(), pose.pitch());
            //? if !forge {
            self.rotation().rotateZ((float) Math.toRadians(-pose.roll()));
            self.getUpVector().set(0, 1, 0).rotate(self.rotation());
            // vanilla's LEFT is (-1, 0, 0): billboards spanned by it (rune circles, glows) must not come out mirrored
            self.getLeftVector().set(-1, 0, 0).rotate(self.rotation());
            //?} else if forge {
            /*if (pose.roll() != 0) {
                // 1.20.1's camera has no roll (Forge rolls the view, see CinematicClient.onCameraAngles): its own vectors,
                // which billboards and particles face by, are rolled the same way here
                float deg = (float) Math.PI / 180f;
                rotation.rotationYXZ(-pose.yaw() * deg, pose.pitch() * deg, pose.roll() * deg);
                forwards.set(0.0F, 0.0F, 1.0F).rotate(rotation);
                up.set(0.0F, 1.0F, 0.0F).rotate(rotation);
                left.set(1.0F, 0.0F, 0.0F).rotate(rotation);
            }
            *///?}
            setPosition(pose.pos());
        }
    }
}
