package moe.nightfall.instrumentality.mc

import java.io.InputStream

import moe.nightfall.instrumentality.ApplicationHost
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation

/**
 * Created on 28/09/15.
 */
class MinecraftApplicationHost extends ApplicationHost {

    // Gets a file from assets/instrumentality/ in a replacable manner.
    override def getResource(resource: String): InputStream = Minecraft.getMinecraft.getResourceManager.getResource(new ResourceLocation("instrumentality:/" + resource)).getInputStream
}
