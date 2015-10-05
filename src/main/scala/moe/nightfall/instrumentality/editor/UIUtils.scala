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
package moe.nightfall.instrumentality.editor

import java.awt.Font

import moe.nightfall.instrumentality.{Loader, ModelCache}
import moe.nightfall.instrumentality.animations.PoseSet
import moe.nightfall.instrumentality.editor.control.{ButtonBarContainerElement, TextButtonElement}
import moe.nightfall.instrumentality.editor.gui.{DownloaderElement, BenchmarkElement, ModelChooserElement, PoseTreeElement}
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.{Display, GL11}
import org.lwjgl.util.vector.Vector2f

object UIUtils {
    // TODO Couldn't this just be two variables?
    var state = new Array[Boolean](2)

    def update(targetPanel: EditElement) {
        val newState = new Array[Boolean](2)
        newState(0) = Mouse.isButtonDown(0)
        newState(1) = Mouse.isButtonDown(1)
        val x = Mouse.getX()
        val y = Display.getHeight() - (Mouse.getY() + 1)
        if (newState(0) != state(0))
            targetPanel.mouseStateChange(x, y, newState(0), 0)
        if (newState(1) != state(1))
            targetPanel.mouseStateChange(x, y, newState(1), 1)
        targetPanel.mouseMove(x, y, newState)
        state = newState
    }

    def createGui(): EditElement = {
        val bbce = new ButtonBarContainerElement(0.0355d)

        val mce = new ModelChooserElement(ModelCache.getLocalModels())
        bbce.setUnderPanel(mce, false)

        bbce.barCore += new TextButtonElement("MdlChoose", 
            // also serves as refresh!
            bbce.setUnderPanel(new ModelChooserElement(ModelCache.getLocalModels()), false)
        )

        bbce.barCore += new TextButtonElement("Benchmark",
            if (Loader.currentFile != null) {
                val mdl = ModelCache.getLocal(Loader.currentFile)
                if (mdl != null)
                    bbce.setUnderPanel(new BenchmarkElement(ModelCache.getLocal(Loader.currentFile)), false)
            }
        )

        bbce.barCore += new TextButtonElement("PoseEdit!", 
            if (Loader.currentFile != null) {
                val mdl = ModelCache.getLocal(Loader.currentFile)
                if (mdl != null)
                    bbce.setUnderPanel(new PoseTreeElement(mdl.poses, bbce), false)
            }
        )

        bbce.barCore += new TextButtonElement("Get PMXs", 
            bbce.setUnderPanel(new DownloaderElement(bbce), false)
        )

        return bbce
    }

    private var sysFont: Font = _
    private var sysFontCreationThread: Thread = _

    def useSystemFont(str: String): Boolean = {
        val ca = str.toCharArray()
        for (c <- ca) {
            if (c == 10) {
                // NOP
            } else if (c == 13) {
                // NOP
            } else if (c == 32) {
                // more NOP
            } else if (UIFont.getCharLoc(c) == -1) {
                // welp, we have an unknown char, assume it's an evil diacritic
                if (sysFont == null) {
                    if (sysFontCreationThread == null) {
                        sysFontCreationThread = new Thread {
                            try {
                                // did I mention: this call is SLOW. Seriously.
                                val f = new Font(null, 0, 24)
                                UISystemFont.scratchFont(f)
                                sysFont = f
                            } catch {
                                case e: Exception =>
                                    System.err.println("Cannot load a international font... nippon gomen nasai... :(")
                                    // TODO: This would be the perfect place to turn on a romanizer as a last resort.
                                    e.printStackTrace()
                            }
                        }
                        sysFontCreationThread.start()
                    }
                    // *sigh* NOP it for now while we load a international char-supporting font
                } else return true
            }
        }
        false
    }

    // yes, this is scalable (in one way or another). TODO: second parameter controls stroke width
    // the default size will be around 6x9u(where u is whatever your opengl perspective says), with 1u spacing on both axis.
    // spaces are 1x9u + the normal spacing on top
    // however, if unknown chars appear, then it will NOT WORK!
    def drawLine(str: String, strokeWidth: Int): Vector2f = {
        val ca = str.toCharArray()
        if (useSystemFont(str))
            return UISystemFont.drawSystemLine(str, sysFont)
        GL11.glPushMatrix()
        var lineX = 0
        for (c <- ca) {
            for (thick <- strokeWidth to 0 by -1) {
                GL11.glColor3d(0, thick / 8d, thick / 5d)
                GL11.glLineWidth(thick + 1)
                UIFont.drawChar(c)
            }
            GL11.glTranslated(7, 0, 0)
            lineX += 7
        }
        GL11.glPopMatrix()
        return new Vector2f(lineX, 8)
    }

    def sizeLine(str: String): Vector2f = {
        if (useSystemFont(str)) {
            val v = UISystemFont.sizeSystemLine(str, sysFont)
            return new Vector2f(v._1.x * v._3.toFloat, v._1.y * v._3.toFloat)
        }
        new Vector2f(7 * str.length, 8)
    }

    def drawText(str0: String, i: Int) {
        var str = str0
        val totalSize = new Vector2f()
        GL11.glPushMatrix()
        while (str.indexOf(10) != -1) {
            val lineSize = drawLine(str.substring(0, str.indexOf(10)), i)
            str = str.substring(str.indexOf(10) + 1)

            totalSize.x = math.max(totalSize.x, lineSize.x)
            totalSize.y += lineSize.y + 1
            GL11.glTranslated(0, lineSize.y, 0)
        }
        val lineSize = drawLine(str, i)
        totalSize.x = math.max(totalSize.x, lineSize.x)
        totalSize.y += lineSize.y
        GL11.glPopMatrix()
    }

    def sizeText(text: String) = {
        var str = text
        val totalSize = new Vector2f()
        while (str.indexOf(10) != -1) {
            val lineSize = sizeLine(str.substring(0, str.indexOf(10)))
            str = str.substring(str.indexOf(10) + 1)

            totalSize.x = math.max(totalSize.x, lineSize.x)
            totalSize.y += lineSize.y + 1
        }
        val lineSize = sizeLine(str)
        totalSize.x = math.max(totalSize.x, lineSize.x)
        totalSize.y += lineSize.y
        totalSize
    }

    def drawBoundedText(text: String, width: Int, height: Int, borderWidth: Int) {
        GL11.glPushMatrix()
        val textSize = sizeText(text)
        var scale: Double = (height - borderWidth) / textSize.getY
        var scaleW: Double = (width - borderWidth) / textSize.getX
        if (scaleW < scale)
            scale = scaleW
        if (scale < 1.7) {
            scale = height / textSize.getY
            scaleW = width / textSize.getX
            if (scaleW < scale)
                scale = scaleW
        } else {
            GL11.glTranslated(borderWidth / 2, borderWidth / 2, 0)
        }
        GL11.glScaled(scale, scale, 1)
        drawText(text, 2)
        GL11.glPopMatrix()
    }
}
