package moe.nightfall.instrumentality.mc;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import moe.nightfall.instrumentality.IMaterialBinder;
import moe.nightfall.instrumentality.Main;
import moe.nightfall.instrumentality.PMXFile;
import moe.nightfall.instrumentality.PMXModel;

public class ClientProxy extends CommonProxy {
	
	@Override
	public void preInit() {
		super.preInit();
		
		try {
			Main.setup();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private double interpolate(double last, double current, float partialTicks) {
		return last + (current - last) * partialTicks;
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerRender(RenderPlayerEvent.Pre event) {
		
		EntityPlayer player = event.entityPlayer;
		
        double x = interpolate(player.lastTickPosX, player.posX, event.partialRenderTick);
        double y = interpolate(player.lastTickPosY, player.posY, event.partialRenderTick);
        double z = interpolate(player.lastTickPosZ, player.posZ, event.partialRenderTick);
        
        double pitch   = interpolate(player.prevRotationPitch  , player.rotationPitch  , event.partialRenderTick);
        double yaw     = interpolate(player.prevRotationYaw    , player.rotationYaw    , event.partialRenderTick);
        double yawHead = interpolate(player.prevRotationYawHead, player.rotationYawHead, event.partialRenderTick);
        double rotBody = interpolate(player.prevRenderYawOffset, player.renderYawOffset, event.partialRenderTick);
        
        x -= RenderManager.renderPosX;
        y -= RenderManager.renderPosY;
        z -= RenderManager.renderPosZ;
        
        PMXModel model = Main.pm[0];
        float scale = 1F / (model.height / player.height);
        
        GL11.glPushMatrix();
        GL11.glRotated(180 - rotBody, 0, 1, 0);
        GL11.glTranslated(x, y - player.height + player.eyeHeight, z);
        GL11.glScalef(scale, scale, scale);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        
        model.render(new IMaterialBinder() {
        	@Override
	        public void bindMaterial(PMXFile.PMXMaterial texture) {
	            GL11.glBindTexture(GL11.GL_TEXTURE_2D, Main.materialTextures.get(texture));
	        }
    	}, Main.shaderBoneTransform);
        
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
        
        event.setCanceled(true);
	}
}
