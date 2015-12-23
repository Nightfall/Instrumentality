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

import moe.nightfall.instrumentality.PMXFile.PMXBone
import moe.nightfall.instrumentality.animations.{Animation, KeyframeAnimationData, PoseAnimation}
import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control._
import moe.nightfall.instrumentality.{Loader, PMXInstance, PMXModel, PoseBoneTransform}
import org.lwjgl.opengl.GL11

class PoseEditElement(val editedPose: String, pm: PMXModel, firstPerson: Boolean) extends EditElement {

    val pmxInst: PMXInstance = new PMXInstance(pm)

    def getEditAnim: KeyframeAnimationData = pm.defaultAnims.allPoses.get(editedPose).get

    // Read-only except in resetFrame.
    // This data exists to simplify calculations like "which frame are we on" and things like that.
    // It is: The current frame as an Int, the PoseAnimation for this frame (even if none exists, it'll be interpolated),
    //        and the Animation object used for the 3D display, based on the PoseAnimation.
    var editingData: (PoseAnimation, Animation) = _

    var editingFrame = 0

    def resetFrame {
        editingData = pm.defaultAnims.createEditAnimation(editedPose, editingFrame / (getEditAnim.lenFrames - 1.0d))
        pmxInst.anim = editingData._2
    }

    resetFrame

    var model: View3DElement = new View3DElement {
        // NOTE: editing of the firstPerson animation triggers special cases in here,
        //       since it has a very specific camera configuration
        colourR = 1
        colourG = 1
        colourB = 1
        colourStrength = 1
        
        override protected def draw3D() {
            GL11.glPushMatrix()
            if (!firstPerson)
                GL11.glTranslated(0, -0.5, 0)
            GL11.glBegin(GL11.GL_LINE_STRIP)
            GL11.glColor3d(0.5, 0.5, 0.5)
            GL11.glVertex3d(-0.5, 0, -0.5)
            GL11.glVertex3d(0.5, 0, -0.5)
            GL11.glVertex3d(0.5, 0, 0.5)
            GL11.glVertex3d(-0.5, 0, 0.5)
            GL11.glVertex3d(-0.5, 0, -0.5)
            GL11.glEnd()
            GL11.glBegin(GL11.GL_LINES)
            GL11.glColor3d(0.5, 0.5, 0.5)
            GL11.glVertex3d(0, 0, -0.5)
            GL11.glVertex3d(0, 0, 0.5)
            GL11.glVertex3d(-0.5, 0, 0)
            GL11.glVertex3d(0.5, 0, 0)
            GL11.glEnd()
            val s = 1 / pmxInst.theModel.height
            GL11.glScaled(s, s, s)
            pmxInst.clearBoneCache()
            if (true) {
                GL11.glEnable(GL11.GL_TEXTURE_2D)
                pmxInst.render(Loader.shaderBoneTransform, 1, 1, 1, 1, 1)
                GL11.glDisable(GL11.GL_TEXTURE_2D)
            }
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
            if (true)
                pmxInst.renderDebug(tView.selectedNode.boneId)
            GL11.glPopMatrix()
        }

        override def mouseMove(x: Int, y: Int, buttons: Array[Boolean]) {
            if (!firstPerson)
                super.mouseMove(x, y, buttons)
        }

        if (firstPerson) {
            rotYaw = 0
            rotPitch = 0
            preTranslateZ = 0.0d
            translateY = 0.0d
            // The model is already scaled down inside the drawing code for both firstPerson and otherwise
            scale = 1
        }
    }


    val tView = new TreeviewElement[PMXBone](new TreeviewElementStructurer[PMXBone] {
        // note: "null" is the Root Node, and is invisible (only it's children are seen)
        override def getNodeName(n: PMXBone): String = n.sensibleName

        override def getChildNodes(n: Option[PMXBone]): Seq[PMXBone] = {
            var parId = -1
            if (n.isInstanceOf[Some[PMXBone]])
                parId = n.get.boneId
            /*return*/ pmxInst.theFile.boneData filter {
                _.parentBoneIndex == parId
            }
        }

        override def onNodeClick(n: PMXBone) {
            /* do nothing */
        }
    })

    val tViewScroll = new ScrollAreaElement(tView)

    val params = new PoseEditParamsElement(this)

    val timeline = new PoseEditTimelineElement(this)

    tView.selectedNode = pmxInst.theFile.boneData(0)

    val udSplitPane = new SplitPaneElement(tViewScroll, params, false, 1d, 0.5d)
    val lrSplitPane = new SplitPaneElement(model, udSplitPane, true, 1d, 0.65d)
    val timelineSplitPane = new SplitPaneElement(lrSplitPane, timeline, false, 1d, 0.75d)
    subElements += timelineSplitPane

    override def layout(): Unit = {
        timelineSplitPane.posX = 0
        timelineSplitPane.posY = 0
        timelineSplitPane.setSize(width, height)
    }

    def getEditPBT: PoseBoneTransform = {
        // What is going on here...?
        val mid = tView.selectedNode.sensibleName.toLowerCase
        // return needed because it *might* be implying that toLowerCase uses the return as a param
        val opbt = editingData._1.hashMap.get(mid)
        // asInstanceOf seems to cause a lot of errors, and getOrElse is just acting weird.
        // Practicality matters.
        if (opbt.isEmpty) {
            val tsf = new PoseBoneTransform()
            editingData._1.hashMap.put(mid, tsf)
            return tsf
        } else {
            return opbt.get
        }
    }

    // This is the code which automatically puts the PoseAnimation into the kfanim.
    // That is all.
    def editMade {
        val anim = getEditAnim
        // Add this as a keyframe based upon what we have
        // (which as you know can be an interpolated state, this is normal for this style of animation program)
        anim.frameMap = anim.frameMap + (editingFrame -> editingData._1)
    }

    override def cleanup() {
        super.cleanup()
        pmxInst.cleanupGL()
    }
}
