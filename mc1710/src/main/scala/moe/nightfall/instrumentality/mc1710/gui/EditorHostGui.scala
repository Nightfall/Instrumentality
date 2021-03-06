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
package moe.nightfall.instrumentality.mc1710.gui

import moe.nightfall.instrumentality.editor.{EditElement, UIUtils}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.opengl.{Display, GL11};

object EditorHostGui {
    var hostedElement: EditElement = null
}

/**
 * Created on 18/08/15.
 */
class EditorHostGui extends GuiScreen {

    var lastTime = System.currentTimeMillis()

    override def initGui() {
        // Only comment the if (hostedElement == null) for testing!
        // If commented, a resource leak happens! --gamemanj
        if (EditorHostGui.hostedElement == null)
            changePanel(UIUtils.createGui())
    }

    override def setWorldAndResolution(mc: Minecraft, width: Int, height: Int) {
        super.setWorldAndResolution(mc, width, height)
        if (EditorHostGui.hostedElement != null)
            EditorHostGui.hostedElement.setSize(Display.getWidth(), Display.getHeight())
    }

    override def drawScreen(xCoord: Int, yCoord: Int, partialTick: Float) {
        // Here's why the FPS was so low - we were limiting ourselves to MC time.
        // Which looks AWFUL.
        val thisTime = System.currentTimeMillis()
        val deltaTime = thisTime - lastTime
        lastTime = thisTime

        UIUtils.update(EditorHostGui.hostedElement)
        EditorHostGui.hostedElement.update(deltaTime / 1000f)

        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glShadeModel(GL11.GL_SMOOTH)
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glLoadIdentity()
        GL11.glOrtho(0, Display.getWidth, Display.getHeight, 0, 0, 1024)
        GL11.glPushMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        UIUtils.prepareForDrawing(Display.getWidth, Display.getHeight)
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        EditorHostGui.hostedElement.draw()
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glShadeModel(GL11.GL_FLAT)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
    }

    def changePanel(newPanel: EditElement) {
        if (EditorHostGui.hostedElement != null)
            EditorHostGui.hostedElement.cleanup()
        EditorHostGui.hostedElement = newPanel
        EditorHostGui.hostedElement.setSize(width, height)
    }
}
