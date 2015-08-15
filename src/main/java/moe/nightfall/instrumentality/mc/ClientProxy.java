package moe.nightfall.instrumentality.mc;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
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
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerRender(RenderPlayerEvent.Pre event) {
		
        double x = event.entity.lastTickPosX + (event.entity.posX - event.entity.lastTickPosX) * event.partialRenderTick;
        double y = event.entity.lastTickPosY + (event.entity.posY - event.entity.lastTickPosY) * event.partialRenderTick;
        double z = event.entity.lastTickPosZ + (event.entity.posZ - event.entity.lastTickPosZ) * event.partialRenderTick;
        
        x -= RenderManager.renderPosX;
        y -= RenderManager.renderPosY;
        z -= RenderManager.renderPosZ;
        
        PMXModel model = Main.pm[0];
        float scale = 1F / (model.height / event.entityPlayer.height);
        
        GL11.glPushMatrix();
        GL11.glTranslated(x, y - event.entityPlayer.height + event.entityPlayer.eyeHeight, z);
        GL11.glScalef(scale, scale, scale);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        
        model.render(new IMaterialBinder() {
        	@Override
	        public void bindMaterial(PMXFile.PMXMaterial texture) {
	            GL11.glBindTexture(GL11.GL_TEXTURE_2D, Main.materialTextures.get(texture));
	        }
    	}, Main.shaderBoneTransform);
        
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glPopMatrix();
        
        event.setCanceled(true);
	}
}
