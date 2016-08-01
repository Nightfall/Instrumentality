/*
 * Copyright (c) 2015, Nightfall Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package moe.nightfall.instrumentality.mc194

import java.io.{FileNotFoundException, InputStream}
import moe.nightfall.instrumentality.ApplicationHost
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import net.minecraft.client.renderer.entity.RenderPlayer
import net.minecraft.client.renderer.entity.RenderManager
import org.lwjgl.opengl.GL11
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.gui.GuiMainMenu

/**
 * Created on 28/09/15.
 */
class MinecraftApplicationHost extends ApplicationHost {

    // Gets a file from assets/instrumentality/ in a replacable manner.
    override def getResource(resource: String): InputStream = {
        try {
            Minecraft.getMinecraft.getResourceManager.getResource(new ResourceLocation("instrumentality:" + resource)).getInputStream
        } catch {
            case e: FileNotFoundException => {
                getClass.getResourceAsStream("/assets/instrumentality/" + resource)
            }
        }
    }

    // Draws the ordinary player. It should be scaled to within a 0-1 vertical range.
    override def drawPlayer(): Unit = {
        val player = Minecraft.getMinecraft.thePlayer
        if (player == null) return
        
        val f1 = player.renderYawOffset
        val f2 = player.rotationYaw
        val f3 = player.rotationPitch
        val f4 = player.prevRotationYawHead
        val f5 = player.rotationYawHead
        
        player.renderYawOffset = 0
        player.rotationYaw = 0
        player.rotationPitch = 0
        player.prevRotationYawHead = 0
        player.rotationYawHead = 0
        
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        ClientProxy.skipRenderEvent = true
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        GL11.glPushMatrix()
        
        GL11.glScalef(1 / 2F, 1 / 2F, 1 / 2F)
        GL11.glRotatef(180, 0, 1, 0)
        // TODO This should be calculatable somehow
        GL11.glTranslated(0, 1.6, 0)

        //RenderManager.instance.renderEntityWithPosYaw(player, 0, 0, 0, 0, 1)
        
        GL11.glPopMatrix()
        GL11.glPopAttrib()
        ClientProxy.skipRenderEvent = false
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        
        player.renderYawOffset = f1
        player.rotationYaw = f2
        player.rotationPitch = f3
        player.prevRotationYawHead = f4
        player.rotationYawHead = f5
        
    }
}
