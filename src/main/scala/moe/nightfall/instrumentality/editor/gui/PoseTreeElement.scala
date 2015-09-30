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

;

import java.io.{DataOutputStream, FileOutputStream}

import moe.nightfall.instrumentality.{Loader, ModelCache}
import moe.nightfall.instrumentality.animations.PoseSet
import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control.{ButtonBarContainerElement, TextButtonElement, TreeviewElement, TreeviewElementStructurer}

/**
 * Created on 11/09/15.
 */
class PoseTreeElement(val targetSet: PoseSet, whereAmI: ButtonBarContainerElement) extends EditElement {
    val treeviewElement = new TreeviewElement(new TreeviewElementStructurer[String] {
        /*
        override def getNodeName(n : String) = n
        override def getChildNodes(n : String) : Seq[String] = {
            val map = for((k, v) <- targetSet.allPoses) yield {
                (if (n == null) {
                    if (!targetSet.poseParents.contains(k)) Some(k)
                } else if (n.equals(targetSet.poseParents.get(k))) Some(k)
                else None).asInstanceOf[Option[String]]
            }
            return map.flatten.toSeq
        }
        override def onNodeClick(n : String) {
            whereAmI.setUnderPanel(new PoseEditElement(PoseTreeElement.this.targetSet.allPoses.get(n).get, ModelCache.getLocal(Loader.currentFile), targetSet.createEditAnimation(n)), false)
        }
        */
        // note: "null" is the Root Node, and is invisible (only it's children are seen)
        override def getNodeName(n: String): String = n

        override def onNodeClick(n: String): Unit =
            whereAmI.setUnderPanel(
                new PoseEditElement(
                    PoseTreeElement.this.targetSet.allPoses.get(n).get,
                    ModelCache.getLocal(Loader.currentFile),
                    targetSet.createEditAnimation(n)),
                noCleanup = false)

        // The nonsense that was here, I cannot understand.
        // Now, Map - Filter - etc., I *CAN* understand!
        override def getChildNodes(n: Option[String]): Iterable[String] =
            targetSet.allPoses.keys.filter(targetSet.poseParents.get(_) == n).toSeq
    })
    
    val scrollArea = new ScrollAreaElement(treeviewElement)

    subElements += scrollArea

    val saveButton = new TextButtonElement("Save", {
        val fos = new FileOutputStream(ModelCache.modelRepository + "/" + Loader.currentFile + "/mmcposes.dat")
        val dos = new DataOutputStream(fos)
        targetSet.save(dos)
        dos.close()
    })

    subElements += saveButton

    override def layout() {
        super.layout()
        treeviewElement.posX = 0
        treeviewElement.posY = 0
        val bbarHeight = (height * 0.1d).toInt
        scrollArea.setSize(width, height - bbarHeight)
        saveButton.posX = 0
        saveButton.posY = height - bbarHeight
        saveButton.setSize(width, bbarHeight)
    }
}
