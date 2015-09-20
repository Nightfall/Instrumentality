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
package moe.nightfall.instrumentality.editor;

import moe.nightfall.instrumentality.Loader
import moe.nightfall.instrumentality.ModelCache
import moe.nightfall.instrumentality.animations.PoseSet
import moe.nightfall.instrumentality.editor.controls.ButtonBarContainerElement
import moe.nightfall.instrumentality.editor.controls.TextButtonElement
import moe.nightfall.instrumentality.editor.guis.BenchmarkElement
import moe.nightfall.instrumentality.editor.guis.ModelChooserElement
import moe.nightfall.instrumentality.editor.guis.PoseTreeElement
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Font

object UIUtils {
    // use when adding new chars to the internal font
    val debugDisableSysFont = false

    // TODO Couldn't this just be two variables?
    var state = new Array[Boolean](2)

    def update(targetPanel : EditElement) {
        val newState = new Array[Boolean](2)
        newState(0) = Mouse.isButtonDown(0)
        newState(1) = Mouse.isButtonDown(1)
        val x = Mouse.getX()
        val y = Display.getHeight() - (Mouse.getY() + 1)
        if (newState(0) != state(0))
            targetPanel.mouseStateChange(x, y, newState(0), false)
        if (newState(1) != state(1))
            targetPanel.mouseStateChange(x, y, newState(1), true)
        targetPanel.mouseMove(x, y, newState)
        state = newState
    }

    def createGui() : EditElement = {
        val bbce = new ButtonBarContainerElement(0.05d)

        // TODO SCALA If this ain't compiling, just wait...
        val mce = new ModelChooserElement(ModelCache.getLocalModels())
        bbce.setUnderPanel(mce, true)

        // TODO SCALA No
        bbce.barCore.addPiece(new TextButtonElement("MdlChoose", new Runnable() {
            override def run() {
                bbce.setUnderPanel(mce, true)
            }
        }))

        bbce.barCore.addPiece(new TextButtonElement("Mikumark 2016", new Runnable() {
            override def run() {
                if (Loader.currentFile != null)
                    bbce.setUnderPanel(new BenchmarkElement(ModelCache.getLocal(Loader.currentFile)), false)
            }
        }))

        val poseSet = new PoseSet()
        bbce.barCore.addPiece(new TextButtonElement("Pose Editor", new Runnable() {
            override def run() {
                if (Loader.currentFile != null)
                    bbce.setUnderPanel(new PoseTreeElement(ModelCache.getLocal(Loader.currentFile).poses, bbce), false)
            }
        }))

        return bbce
    }

    private var sysFont : Font = _
    private var sysFontCreationThread : Thread = _

    // yes, this is scalable (in one way or another). TODO: second parameter controls stroke width
    // the default size will be around 6x9u(where u is whatever your opengl perspective says), with 1u spacing on both axis.
    // spaces are 1x9u + the normal spacing on top
    // however, if unknown chars appear, then it will NOT WORK!
    def drawLine(str : String, strokeWidth : Int) : Vector2f = {
        val ca = str.toCharArray()
        if (!debugDisableSysFont) {
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
                                      case e : Exception =>
                                      System.err.println("Cannot load a international font... nippon gomen nasai... :(")
                                      // TODO: This would be the perfect place to turn on a romanizer as a last resort.
                                      e.printStackTrace()
                                  }
                            }
                            sysFontCreationThread.start()
                        }
                        // *sigh* NOP it for now while we load a international char-supporting font
                    } else return UISystemFont.drawSystemLine(str, sysFont)
                }
            }
        }
        GL11.glPushMatrix()
        var lineX = 0
        for (c <- ca) {
            // TODO SCALA 'to' or 'until'? Nobody knows (>= 0)
            for (thick <- strokeWidth to 0 by -1) {
                GL11.glColor3d(0, thick / 8d, thick / 5d)
                GL11.glLineWidth(thick + 1)
                UIFont.drawChar(c)
            }
            GL11.glTranslated(7, 0, 0)
            lineX += 7
        }
        GL11.glPopMatrix()
        return new Vector2f(lineX, 10)
    }

    def drawText(str0 : String, i : Int) {
        var str = str0
        val totalSize = new Vector2f()
        GL11.glPushMatrix()
        while (str.indexOf(10) != -1) {
            val lineSize = drawLine(str.substring(0, str.indexOf(10)), i)
            str = str.substring(str.indexOf(10) + 1)

            totalSize.x = Math.max(totalSize.x, lineSize.x)
            totalSize.y += lineSize.y
            GL11.glTranslated(0, lineSize.y, 0)
        }
        totalSize.x = Math.max(totalSize.x, drawLine(str, i).x)
        GL11.glPopMatrix()
    }
}
