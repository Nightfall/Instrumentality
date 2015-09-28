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
package moe.nightfall.instrumentality

import java.io.{InputStream, BufferedReader, InputStreamReader}

import moe.nightfall.instrumentality.editor.{EditElement, UIFont, UIUtils}
import org.lwjgl.input.{Keyboard, Mouse}
import org.lwjgl.opengl.{Display, DisplayMode, GL11}

/**
 * The testbench.
 * Minecraft takes way too long to start for UI testing to be done reasonably under it.
 * So: Test the majority of code under this, and use Minecraft occasionally and make sure everything matches up.
 * Ofc, this method doesn't work for the integration code, so test that using Minecraft.
 * Created on 24/07/15.
 */
object Main extends App {
    new Main().startWorkbench()
}

class Main extends ApplicationHost {
    private var currentPanel: EditElement = _

    def startWorkbench() {
        val consoleReader = new BufferedReader(new InputStreamReader(System.in))

        var (scrWidth, scrHeight) = (800, 600)
        Display.setTitle("Instrumentality: PMX Animation Workbench")
        Display.setDisplayMode(new DisplayMode(scrWidth, scrHeight))
        Display.setResizable(true)
        Display.create()
        Mouse.create()

        Loader.setup(this)

        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthFunc(GL11.GL_LEQUAL)

        var frameEndpoint = System.currentTimeMillis()

        Keyboard.create()
        var deltaTime = 0.1d

        onSizeChange(scrWidth, scrHeight)

        changePanel(UIUtils.createGui())

        while (!Display.isCloseRequested) {
            if (Display.wasResized()) {
                scrWidth = Display.getWidth
                scrHeight = Display.getHeight
                onSizeChange(scrWidth, scrHeight)
            }
            val frameStart = System.currentTimeMillis()
            GL11.glClearColor(0.5f, 0.5f, 0.5f, 1.0f)
            GL11.glClearDepth(1.0f)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)
            GL11.glLoadIdentity()

            Keyboard.poll()
            Mouse.poll()

            doUpdate(deltaTime)

            doDraw()

            Display.update()

            val currentTime = System.currentTimeMillis()
            // Frame start is frameEndpoint-20 (note that the delta does include
            // the sleep)
            val delta = (currentTime - (frameEndpoint - 30)).toInt
            deltaTime = delta / 1000.0d
            doUpdate(deltaTime)

            val v = frameEndpoint - currentTime
            if (v > 1)
                Thread.sleep(v)
            //Display.setTitle("I-PMXAW: FrameTime " + (currentTime - frameStart) + "ms");
            frameEndpoint = currentTime + 30
        }
        Display.destroy()
    }

    def onSizeChange(scrWidth: Int, scrHeight: Int) {
        GL11.glViewport(0, 0, scrWidth, scrHeight)
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glLoadIdentity()
        GL11.glOrtho(0, scrWidth, scrHeight, 0, 0, 1024)
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        if (currentPanel != null)
            currentPanel.setSize(scrWidth, scrHeight)
    }

    def doUpdate(dt: Double) {
        UIUtils.update(currentPanel)
        currentPanel.update(dt)
    }

    private def doDraw() {
        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        currentPanel.draw(Display.getWidth, Display.getHeight)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)
    }

    def changePanel(newPanel: EditElement) {
        currentPanel = newPanel
        currentPanel.setSize(Display.getWidth, Display.getHeight)
    }

    // Gets a file from assets/instrumentality/.
    override def getResource(resource: String): InputStream = getClass.getResourceAsStream("/assets/instrumentality/" + resource)
}
