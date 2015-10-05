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
import moe.nightfall.instrumentality.{Loader, PMXInstance, PMXModel, PoseBoneTransform}
import moe.nightfall.instrumentality.animations.{Animation, PoseAnimation}
import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control._
import org.lwjgl.opengl.GL11

class PoseEditElement(val editedPose: PoseAnimation, pm: PMXModel, editAnimation: Animation) extends EditElement {

    val pmxInst: PMXInstance = new PMXInstance(pm)
    pmxInst.anim = editAnimation

    var model: View3DElement = new View3DElement {
        override protected def draw3D() {
            GL11.glPushMatrix()
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
            if (params.showModel.checked) {
                GL11.glEnable(GL11.GL_TEXTURE_2D)
                pmxInst.render(Loader.shaderBoneTransform, 1, 1, 1, 2)
                GL11.glDisable(GL11.GL_TEXTURE_2D)
            }
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
            if (params.showDebug.checked)
                pmxInst.renderDebug(tView.selectedNode.boneId)
            GL11.glPopMatrix()
        }
    }

    val tView = new TreeviewElement[PMXBone](new TreeviewElementStructurer[PMXBone] {
        // note: "null" is the Root Node, and is invisible (only it's children are seen)
        override def getNodeName(n: PMXBone): String = n.globalName

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

    val params: PoseEditParamsElement = new PoseEditParamsElement(this)

    tView.selectedNode = pmxInst.theFile.boneData(0)

    val udSplitPane = new SplitPaneElement(tViewScroll, params, false, 1d)
    val lrSplitPane = new SplitPaneElement(model, udSplitPane, true, 1d)
    subElements += lrSplitPane

    override def layout(): Unit = {
        lrSplitPane.posX = 0
        lrSplitPane.posY = 0
        lrSplitPane.setSize(width, height)
    }

    def getEditPBT: PoseBoneTransform = {
        // What is going on here...?
        val mid = tView.selectedNode.globalName.toLowerCase
        // return needed because it *might* be implying that toLowerCase uses the return as a param
        val opbt = editedPose.hashMap.get(mid)
        // asInstanceOf seems to cause a lot of errors, and getOrElse is just acting weird.
        // Practicality matters.
        if (opbt.isEmpty) {
            val tsf = new PoseBoneTransform()
            editedPose.hashMap.put(mid, tsf)
            return tsf
        } else {
            return opbt.get
        }
    }

    override def cleanup() {
        super.cleanup()
        pmxInst.cleanupGL()
    }
}
