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
package moe.nightfall.instrumentality.mc.gui

;

import moe.nightfall.instrumentality.editor.{EditElement, UIFont, UIUtils}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.{Display, GL11};

/**
 * Created on 18/08/15.
 */
class EditorHostGui extends GuiScreen {

    var hostedElement: EditElement = null

    override def initGui() {
        if (UIFont.fontDB == null)
            UIFont.setFont(Minecraft.getMinecraft.mcDefaultResourcePack.getInputStream(new ResourceLocation("instrumentality:/font.txt")))
        if (hostedElement == null)
            changePanel(UIUtils.createGui())
    }

    override def setWorldAndResolution(mc: Minecraft, width: Int, height: Int) {
        super.setWorldAndResolution(mc, width, height)
        if (hostedElement != null)
            hostedElement.setSize(Display.getWidth(), Display.getHeight())
    }

    override def drawScreen(xCoord: Int, yCoord: Int, partialTick: Float) {
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glLoadIdentity()
        GL11.glOrtho(0, Display.getWidth(), Display.getHeight(), 0, 0, 1024)
        GL11.glPushMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        hostedElement.draw(Display.getWidth(), Display.getHeight())
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
    }

    override def updateScreen() {
        UIUtils.update(hostedElement)
        hostedElement.update(0.05d)
    }

    def changePanel(newPanel: EditElement) {
        if (hostedElement != null)
            hostedElement.cleanup()
        hostedElement = newPanel
        hostedElement.setSize(width, height)
    }
}
