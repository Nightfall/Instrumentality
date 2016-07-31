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

package moe.nightfall.instrumentality.editor.gui

import moe.nightfall.instrumentality.animations.NewPCAAnimation
import moe.nightfall.instrumentality.editor.control._
import moe.nightfall.instrumentality.editor.{EditElement, UIUtils}
import moe.nightfall.instrumentality.{Loader, ModelCache, PMXInstance}
import org.lwjgl.opengl.{GL11, GL14}
import org.lwjgl.BufferUtils

/**
 * Created on 25/08/15, ported to Scala on 2015-09-20. Oh, and our date formats are inconsistent.
 * availableModels must contain at least null.
 * Also note that I intend for this to perform loading in the background.
 */
class ModelChooserElement(val availableModels: Seq[String], powerlineContainerElement: PowerlineContainerElement) extends EditElement {
    private val availableModelUnits = new Array[PMXInstance](availableModels.length)
    
    availableModels.zipWithIndex.foreach { case (k, v) =>
        if (k != null) {
            val mdl = ModelCache.getLocal(k)
            if (mdl != null) {
                availableModelUnits(v) = new PMXInstance(mdl)
                availableModelUnits(v).anim = new NewPCAAnimation(mdl.defaultAnims)
            }
        }
    }
    
    // offset for rendering to get animation
    var renderOffset = 0D
    // Used to prevent a total failure.
    var slowLoad = 0
    
    // No border, no shadow
    borderWidth = 0
    shadowWidth = 0

    private var mainRotary = new View3DElement {
        
        override def rotate() = ()  
        
        override def draw3D(): Unit = {
            def renderModel(index: Int, clipping: Float) {
                if (index > slowLoad || index < 0 || index >= availableModelUnits.length) return
                if (availableModels(index) == null) {
                    // Player
                    GL11.glPushMatrix()
                    GL11.glEnable(GL11.GL_CLIP_PLANE0)
                    val buffer = BufferUtils.createDoubleBuffer(4).put(Array(0D, -1D, 0D, clipping))
                    buffer.rewind()
                    GL11.glClipPlane(GL11.GL_CLIP_PLANE0, buffer)
                    Loader.applicationHost.drawPlayer()
                    GL11.glDisable(GL11.GL_CLIP_PLANE0)
                    GL11.glPopMatrix()
                } else {
                    val mu = availableModelUnits(index)
                    if (mu != null) {
                        GL11.glPushMatrix()
                        val scale = 1 / mu.theModel.height
                        GL11.glScaled(scale, scale, scale)
                        mu.render(1, 1, 1, clipping, 0.25F)
                        GL11.glPopMatrix()
                    }
                }
            }
            
            def drawText(index: Int) {
                if (index < 0 || index >= availableModelUnits.length) return
                val name = if (availableModels(index) == null) "Default" else {
                    val mu = availableModelUnits(index)
                    if (mu != null) mu.theFile.globalCharname
                    else return
                }

                GL11.glPushMatrix()
                GL11.glTranslated(0, 1.1d, 0)
                GL11.glScaled(-0.1d, -0.1d, 0.1d)
                GL11.glScaled(0.125d, 0.125d, 0.125d)
                val nameSize = UIUtils.sizeText(name)
                val textScale = if (nameSize.getX > 64) 1 / (((nameSize.getX - 64) / 64) + 1) else 1
                GL11.glScaled(textScale, textScale, 1)
                GL11.glTranslated(-nameSize.getX / 2, -nameSize.getY, 0)
                
                UIUtils.drawText(name)
                GL11.glPopMatrix()
            }
            
            GL11.glPushMatrix()
            GL11.glRotatef(180, 0, 1, 0)
            GL11.glTranslatef(0, 0, 1)
            
            val current = availableModels.indexOf(Loader.currentFile)
            
            // Draw text
            for (offset <- -2 to 2) {
                GL11.glPushMatrix()
                GL11.glTranslated((offset + renderOffset) * 0.75, 0, math.abs(offset + renderOffset) * 0.5 - 1)
                drawText(current + offset)
                GL11.glPopMatrix()
            }
            
            def renderModels() {
                for (offset <- (if (renderOffset > 0) -3 else -2) to (if (renderOffset < 0) 3 else 2)) {
                    GL11.glPushMatrix()
                    GL11.glTranslated((offset + renderOffset) * 0.75, 0, math.abs(offset + renderOffset) * 0.5 - 1)
                    // Selected
                    if (offset == 0) {
                        GL11.glRotated(rotYaw, 0, 1, 0)
                    }
                    val fadeIn = if (math.abs(offset) == 3) {
                        math.abs(renderOffset).toFloat
                    } else if (renderOffset < 0 && offset == -2 || renderOffset > 0 && offset == 2) {
                        1 - math.abs(renderOffset).toFloat
                    } else 1
                    
                    renderModel(current + offset, fadeIn)
                    GL11.glPopMatrix()
                }
            }

            renderModels()
            GL11.glScaled(1, -1, 1)
            renderModels()
            
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glColor4f(1, 1, 1, 0.5F)
            GL11.glPopMatrix()
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glVertex3d(-20, 0, -20)
            GL11.glVertex3d(20, 0, -20)
            GL11.glVertex3d(20, 0, 20)
            GL11.glVertex3d(-20, 0, 20)
            GL11.glEnd()
            GL11.glColor4f(1, 1, 1, 1)
            GL11.glDisable(GL11.GL_BLEND)

            slowLoad += 1
            if (slowLoad > availableModelUnits.length)
                slowLoad = availableModelUnits.length
        }
    }

    mainRotary.translateY = -0.6
    mainRotary.scale = 3

    subElements += mainRotary
    
    private var buttonLeft = new ArrowButtonElement(180, updateModelPosition(availableModels(getFB._2), 1)) 
    private var buttonRight = new ArrowButtonElement(0, updateModelPosition(availableModels(getFB._1), -1))
    
    private def updateModelPosition(next: String, offset: Int) {
        if (next != Loader.currentFile) {
            Loader.setCurrentFile(next)
            mainRotary.rotYaw = 0
            renderOffset = offset
            updateButtons()
        }
    }

    private var buttonbar = Array[TextButtonElement](
        new TextButtonElement("Downloader", {
            val l = new DownloaderElement(powerlineContainerElement)
            powerlineContainerElement.addAndGo("PMX Downloader", l)
        }),
        new TextButtonElement("Benchmark", {
            if (Loader.currentFile != null) {
                val mdl = ModelCache.getLocal(Loader.currentFile)
                if (mdl != null) {
                    val l = new BenchmarkElement(mdl)
                    powerlineContainerElement.addAndGo("Benchmark", l)
                }
            }
        }),
        new TextButtonElement("Animations", {
            if (Loader.currentFile != null) {
                val mdl = ModelCache.getLocal(Loader.currentFile)
                if (mdl != null) {
                    val l = new PoseTreeElement(mdl, powerlineContainerElement)
                    powerlineContainerElement.addAndGo("Animations", l)
                }
            }
        })
    )
    subElements ++= buttonbar += buttonRight += buttonLeft
    
    private def updateButtons() {
        val isDefault = Loader.currentFile == null
        buttonbar(1).disabled = isDefault
        buttonbar(2).disabled = isDefault
    }

    // TODO This method does not comply with my standards because of the overuse of tuples, divide into two methods pls
    private def getFB: (Int, Int) = {
        val point = availableModels.zipWithIndex.filter(_._1 == Loader.currentFile)
        val index = point.head._2
        val next = if (index == (availableModelUnits.length - 1)) index else index + 1
        if (index == 0)
            return (index, next)
        (index - 1, next)
    }

    override def layout() {
        
        var pos = width - 35
        buttonbar.view.zipWithIndex.foreach { case (button, index) =>       
            val buttonWidth = math.ceil(UIUtils.sizeText(button.text).getX).toInt * 5
            pos -= buttonWidth
            button.setSize(buttonWidth, 48)
            button.posX = pos
            button.posY = height - button.height - 5
        }
        
        buttonLeft.setSize(30, height)
        buttonRight.posY = 0
        buttonRight.posX = 0
        
        buttonRight.setSize(30, height)
        buttonRight.posY = 0
        buttonRight.posX = width - 30

        mainRotary.posX = 0
        mainRotary.posY = 0
        mainRotary.setSize(width, height)
        
        // Don't know if this is needed, just in case it was changed elsewhere...
        updateButtons()
    }

    override def update(dT: Double) = {
        
        // Fancy animation
        if (renderOffset > 0) {
            renderOffset -= dT * 2
            if (renderOffset < 0) renderOffset = 0
        } else if (renderOffset < 0) {
            renderOffset += dT * 2
            if (renderOffset > 0) renderOffset = 0
        }
        
        availableModels.zipWithIndex.foreach(kv => {
            if (availableModelUnits(kv._2) != null)
                availableModelUnits(kv._2).update(dT)
        })
    }

    override def cleanup() = {
        availableModels.zipWithIndex.foreach(kv => {
            if (availableModelUnits(kv._2) != null)
                availableModelUnits(kv._2).cleanupGL()
        })
    }
}
