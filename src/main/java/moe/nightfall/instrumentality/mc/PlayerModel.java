package moe.nightfall.instrumentality.mc;

import moe.nightfall.instrumentality.IMaterialBinder;
import moe.nightfall.instrumentality.Main;
import moe.nightfall.instrumentality.PMXFile;
import moe.nightfall.instrumentality.PMXModel;
import moe.nightfall.instrumentality.animations.*;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class PlayerModel {

    public final PMXFile file;
    public final PMXModel pmxModel;

    public final LibraryAnimation libanim;
    public final PlayerControlAnimation pcanim;

    private IAnimation useAnimation, idleAnimation;

    public PlayerModel(PMXFile file, int groupSize) {
        this.file = file;
        this.pmxModel = new PMXModel(file, groupSize);

        this.libanim = new LibraryAnimation();
        this.libanim.transitionValue = 1F;

        WalkingAnimation wa = new WalkingAnimation();
        StrengthMultiplyAnimation smaW = new StrengthMultiplyAnimation(wa);

        this.pcanim = new PlayerControlAnimation(wa, smaW);
        this.pcanim.walkingFlag = true;

        this.pmxModel.anim = new OverlayAnimation(smaW, this.pcanim, this.libanim);

        this.useAnimation = Main.animLibs[1].getPose("use");
        this.idleAnimation = Main.animLibs[1].getPose("idle");
    }

    public void update(double v) {
        pmxModel.update(v);
    }

    private double interpolate(double last, double current, float partialTicks) {
        return last + (current - last) * partialTicks;
    }

    public void render(EntityPlayer player, double x, double y, double z, float partialTick) {

        // TODO: make this per-model somehow.
        float adjustFactor = player.isSneaking() ? 0.1f : 0.05f;
        float scale = 1F / (pmxModel.height / player.height);

        double rotBody = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTick);

        GL11.glPushMatrix();
        GL11.glRotated(180 - rotBody, 0, 1, 0);
        GL11.glTranslated(x, ((y - player.height) + player.eyeHeight) + adjustFactor, z);
        GL11.glScalef(scale, scale, scale);
        // I fixed the triangle order, but skirts do not play well with culling
        GL11.glDisable(GL11.GL_CULL_FACE);

        pmxModel.render(new IMaterialBinder() {
            @Override
            public void bindMaterial(PMXFile.PMXMaterial texture) {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, Main.materialTextures.get(texture));
            }
        }, Main.shaderBoneTransform);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    public void apply(EntityPlayer player, float partialTick) {

        double pitch = interpolate(player.prevRotationPitch, player.rotationPitch, partialTick);
        double rotBody = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTick);

        // The interpolated values act a bit weird
        pcanim.lookLR = (float) ((player.renderYawOffset - player.rotationYaw) / 180f);
        pcanim.lookUD = (float) (-pitch / 140.0f);
        pcanim.bodyRotation = (float) Math.toRadians(rotBody);
        pcanim.sneakStateTarget = player.isSneaking() ? 1.0f : 0.0f;

        if (player.isSwingInProgress) {
            if (libanim.getTarget() != useAnimation)
                libanim.setCurrentPose(useAnimation, 0.2f, false);
        } else {
            if (libanim.getTarget() != idleAnimation)
                libanim.setCurrentPose(idleAnimation, 0.1f, false);
        }

        if (player.capabilities.isFlying)
            pcanim.sneakStateTarget = -1.0f;
        pcanim.walkingFlag = ((player.lastTickPosX != player.posX) || (player.lastTickPosZ != player.posZ));
        double xSpd = Math.abs(player.lastTickPosX - player.posX);
        double zSpd = Math.abs(player.lastTickPosZ - player.posZ);
        float spdMul = 5.0f;
        if (player.isSneaking())
            spdMul = 10.0f;
        pcanim.walking.speed = spdMul * ((float) Math.sqrt((xSpd * xSpd) + (zSpd * zSpd)));
    }

    public void cleanupGL() {
        pmxModel.cleanupGL();
    }
}
