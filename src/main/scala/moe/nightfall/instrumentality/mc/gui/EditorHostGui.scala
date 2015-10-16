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

import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer
import de.matthiasmann.twl.{GUI, Widget}
import moe.nightfall.instrumentality.Loader
import moe.nightfall.instrumentality.editor.UIUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.{Display, GL11}

/**
 * Created on 18/08/15.
 */
class EditorHostGui extends GuiScreen {

    var lastTime = System.currentTimeMillis()
    var hostedElement: GUI = null
    var hostedRenderer: LWJGLRenderer = null

    override def initGui() {
        if (hostedElement == null) {
            hostedRenderer = new LWJGLRenderer()
            hostedElement = new GUI(hostedRenderer)
            changePanel(UIUtils.createGui())
        }
    }

    override def setWorldAndResolution(mc: Minecraft, width: Int, height: Int) {
        super.setWorldAndResolution(mc, width, height)
        if (hostedElement != null)
            hostedElement.setSize(Display.getWidth(), Display.getHeight())
    }

    override def drawScreen(xCoord: Int, yCoord: Int, partialTick: Float) {
        // Here's why the FPS was so low - we were limiting ourselves to MC time.
        // Which looks AWFUL.
        val thisTime = System.currentTimeMillis()
        val deltaTime = thisTime - lastTime
        lastTime = thisTime

        GL11.glShadeModel(GL11.GL_SMOOTH)
        hostedElement.update()
        GL11.glShadeModel(GL11.GL_FLAT)
    }

    def changePanel(newPanel: Widget) {
        if (hostedElement.getRootPane != null)
            hostedElement.getRootPane.destroy()
        hostedElement.setRootPane(newPanel)
        hostedElement.setSize(width, height)
    }
}
