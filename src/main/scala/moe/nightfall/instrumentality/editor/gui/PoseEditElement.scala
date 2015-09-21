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

import org.lwjgl.opengl.GL11
import moe.nightfall.instrumentality.Loader
import moe.nightfall.instrumentality.PMXInstance
import moe.nightfall.instrumentality.PMXModel
import moe.nightfall.instrumentality.PoseBoneTransform
import moe.nightfall.instrumentality.animations.Animation
import moe.nightfall.instrumentality.animations.PoseAnimation
import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control.TreeviewElement
import moe.nightfall.instrumentality.editor.control.View3DElement
import moe.nightfall.instrumentality.editor.control.TreeviewElementStructurer
import moe.nightfall.instrumentality.PMXFile.PMXBone

class PoseEditElement(val editedPose : PoseAnimation, pm : PMXModel, editAnimation : Animation) extends EditElement {
    
    val pmxInst : PMXInstance = new PMXInstance(pm)
    pmxInst.anim = editAnimation
    
    var model : View3DElement = new View3DElement {
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
              if (params.showModel.checked)
                  pmxInst.render(Loader.shaderBoneTransform, 1, 1, 1, 2)
              GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
              if (params.showDebug.checked)
                  pmxInst.renderDebug(tView.selectedNode.boneId)
              GL11.glPopMatrix()
        }
    }
    subElements += model
        
    val tView = new TreeviewElement[PMXBone](new TreeviewElementStructurer[PMXBone] {
        // note: "null" is the Root Node, and is invisible (only it's children are seen)
        override def getNodeName(n: Option[PMXBone]): String = n.get.globalName
    
        override def getChildNodes(n: Option[PMXBone]): Seq[PMXBone] = {
            var parId = -1
            if (n != null)
                parId = n.get.boneId
            /*return*/ pmxInst.theFile.boneData filter { _.parentBoneIndex == parId}
        }
    
        override def onNodeClick(n: Option[PMXBone]) { /* do nothing */ }
    })
    
    val params : PoseEditParamsElement = new PoseEditParamsElement(this)
    
    tView.selectedNode = pmxInst.theFile.boneData(0)
    subElements += tView
    subElements += params

    override def layout() {
        model.posX = 0
        model.posY = 0
        val hSplit = (width * 0.40d).toInt
        val vSplit = (height * 0.8d).toInt
        model.setSize(hSplit, height)
        tView.posX = hSplit
        tView.posY = 0
        tView.setSize(width - hSplit, vSplit)
        params.posX = hSplit
        params.posY = vSplit
        params.setSize(width - hSplit, height - vSplit)
    }

    def getEditPBT: PoseBoneTransform = {
        val mid = tView.selectedNode.globalName.toLowerCase
        editedPose.hashMap.getOrElse(mid, () => {
            val tsf = new PoseBoneTransform()
            editedPose.hashMap.put(mid, tsf)
            tsf
        }).asInstanceOf[PoseBoneTransform]
    }

    override def cleanup() {
        super.cleanup()
        pmxInst.cleanupGL()
    }
}
