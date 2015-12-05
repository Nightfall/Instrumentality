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
import moe.nightfall.instrumentality.editor.{UIUtils, EditElement, UIFont}
import moe.nightfall.instrumentality.{Loader, ModelCache, PMXInstance}
import org.lwjgl.opengl.GL11

/**
 * Created on 25/08/15, ported to Scala on 2015-09-20. Oh, and our date formats are inconsistent.
 * availableModels must contain at least null.
 * Also note that I intend for this to perform loading in the background.
 */
class ModelChooserElement(val availableModels: Seq[String], powerlineContainerElement: PowerlineContainerElement) extends EditElement {
    private val availableModelUnits = new Array[PMXInstance](availableModels.length)

    // Used for angle calculations in the Rotary
    private val sliceSize = math.Pi / (0.5 * availableModelUnits.length)
    private val sliceOfs = -math.Pi
    private val rotaryScale = availableModelUnits.length / 4.0d

    private var angleValue = 0d
    private var angleTarget = 0d

    // Used to prevent a total failure.
    var slowLoad = 0

    availableModels.zipWithIndex.foreach(kv => {
        if (kv._1 != null) {
            val mdl = ModelCache.getLocal(kv._1)
            availableModelUnits(kv._2) = new PMXInstance(mdl)
            availableModelUnits(kv._2).anim = new NewPCAAnimation(mdl.anims)
        }
    })

    private var mainRotary = new View3DElement {
        override protected def draw3D(): Unit = {
            GL11.glTranslated(0, 0, -rotaryScale)
            GL11.glRotated(math.toDegrees(-angleValue), 0, 1, 0)
            GL11.glLineWidth(4)
            GL11.glBegin(GL11.GL_LINE_LOOP)
            GL11.glColor3b(0, 0, 0)
            availableModels.zipWithIndex.foreach(kv => {
                val angle = sliceOfs + (sliceSize * kv._2)
                GL11.glVertex3d(math.sin(angle) * rotaryScale, 0, math.cos(angle) * rotaryScale)
            })
            GL11.glEnd()
            GL11.glBegin(GL11.GL_LINES)
            GL11.glColor3b(0, 0, 0)
            GL11.glVertex3d(0, 0, 0)
            GL11.glVertex3d(math.sin(angleValue) * rotaryScale, 0, math.cos(angleValue) * rotaryScale)
            GL11.glEnd()
            availableModels.zipWithIndex.foreach(kv => {
                val angle = sliceOfs + (sliceSize * kv._2)
                GL11.glPushMatrix()
                GL11.glTranslated(math.sin(angle) * rotaryScale, 0, math.cos(angle) * rotaryScale)
                GL11.glRotated(math.toDegrees(angle) + 180, 0, 1, 0)
                if (slowLoad > kv._2) {
                    if (availableModelUnits(kv._2) != null) {
                        val scale = 1 / availableModelUnits(kv._2).theModel.height
                        GL11.glPushMatrix()
                        GL11.glScaled(scale, scale, scale)
                        availableModelUnits(kv._2).render(Loader.shaderBoneTransform, 1, 1, 1, 1.1f, 1.1f)
                        GL11.glPopMatrix()
                        GL11.glTranslated(0, 1.1d, 0)
                        GL11.glScaled(-0.1d, -0.1d, 0.1d)
                        GL11.glScaled(0.125d, 0.125d, 0.125d)
                        val nameSize = UIUtils.sizeText(kv._1)
                        val textScale = if (nameSize.getX > 64) 1 / (((nameSize.getX - 64) / 64) + 1) else 1
                        GL11.glScaled(textScale, textScale, 1)
                        GL11.glTranslated(-nameSize.getX / 2, -nameSize.getY, 0)
                        UIUtils.drawText(kv._1)
                    } else {
                        if (kv._1 == null) {
                            // Player
                            Loader.applicationHost.drawPlayer()
                        } else {
                            // Unloaded (?)
                            GL11.glTranslated(0.5, 0.5, 0)
                            GL11.glScaled(-0.1d, -0.1d, 0.1d)
                            GL11.glScaled(0.125d, 0.125d, 0.125d)
                            UIUtils.drawText("Loading...")
                        }
                    }
                } else {
                    GL11.glTranslated(0.5, 0.5, 0)
                    GL11.glScaled(-0.1d, -0.1d, 0.1d)
                    GL11.glScaled(0.125d, 0.125d, 0.125d)
                    UIUtils.drawText("Please wait")
                }
                GL11.glPopMatrix()
            })
            slowLoad += 1
            if (slowLoad > availableModelUnits.length)
                slowLoad = availableModelUnits.length
        }
    }

    mainRotary.translateY = -0.6
    mainRotary.scale = 3

    subElements += mainRotary

    private var buttonbar = Array[EditElement](
        new ArrowButtonElement(180, {
            Loader.setCurrentFile(availableModels(getFB._1))
        }),
        new ArrowButtonElement(0, {
            Loader.setCurrentFile(availableModels(getFB._2))
        }),
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
    subElements ++= buttonbar

    private def getFB: (Int, Int) = {
        val point = availableModels.zipWithIndex.filter(_._1 == Loader.currentFile)
        val current = point.head
        val next = if (current._2 == (availableModelUnits.length - 1)) 0 else current._2 + 1
        if (current._2 == 0)
            return (availableModelUnits.length - 1, next)
        (current._2 - 1, next)
    }

    override def layout() = {
        val y = height / 8

        var genWidth = width / buttonbar.length
        var pos = 0
        for ((button, index) <- buttonbar.view.zipWithIndex) {
            button.posX = pos
            button.posY = y * 7
            var usedWidth = genWidth
            if (button.isInstanceOf[ArrowButtonElement]) {
                // recalculate based on remaining. (This will not work if an arrow is last!!!)
                usedWidth = y
                genWidth = (width - (pos + usedWidth)) / ((buttonbar.length - index) - 1)
            }
            button.setSize(usedWidth, y)
            pos += button.width
        }

        mainRotary.posX = 0
        mainRotary.posY = 0
        mainRotary.setSize(width, y * 7)
    }

    // Both values are from -Pi to Pi.
    // If there is a difference of > Pi between them, it's more efficient to go in reverse
    private def aComp(angleValue: Double, angleTarget: Double): Double = if (math.abs(angleValue - angleTarget) <= math.Pi) angleValue - angleTarget else angleTarget - angleValue

    private def aFlow(angle: Double): Double = {
        if (angle < -math.Pi)
            return angle + (math.Pi * 2)
        if (angle > math.Pi)
            return angle - (math.Pi * 2)
        angle
    }

    override def update(dT: Double) = {
        availableModels.zipWithIndex.filter(_._1 == Loader.currentFile).foreach(kv => angleTarget = sliceOfs + (sliceSize * kv._2))
        availableModels.zipWithIndex.foreach(kv => {
            if (availableModelUnits(kv._2) != null)
                availableModelUnits(kv._2).update(dT)
        })
        if (aComp(angleValue, angleTarget) < 0) {
            angleValue += dT
            angleValue = aFlow(angleValue)
            if (aComp(angleValue, angleTarget) > 0)
                angleValue = angleTarget
        } else {
            angleValue -= dT
            angleValue = aFlow(angleValue)
            if (aComp(angleValue, angleTarget) < 0)
                angleValue = angleTarget
        }
    }

    override def cleanup() = {
        availableModels.zipWithIndex.foreach(kv => {
            if (availableModelUnits(kv._2) != null)
                availableModelUnits(kv._2).cleanupGL()
        })
    }
}
