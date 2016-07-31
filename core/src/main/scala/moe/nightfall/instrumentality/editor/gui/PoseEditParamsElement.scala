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

import moe.nightfall.instrumentality.PoseBoneTransform
import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control.{AdjusterElement, AdjusterElementData}

import scala.collection.mutable.MutableList

/**
 * Created on 10/09/15.
 */
class PoseEditParamsElement(val parentPE: PoseEditElement) extends EditElement {
    val allAdjusters = MutableList[AdjusterElement]()

    //    Seq("X0", "Y0", "Z0", "X1", "Y1", null, "X2", null, null, "TX0", "TY0", "TX0") zipWithIndex {


    private def createElement(name: String, get: PoseBoneTransform => Double, set: (PoseBoneTransform, Double) => Unit): AdjusterElement =
        new AdjusterElement(name + ":", new AdjusterElementData {
            override def value_=(newValue: Double) {
                set(parentPE.getEditPBT, newValue)
                parentPE.editMade
            }

            override def value: Double = get(parentPE.getEditPBT)
        })

    allAdjusters +=
        createElement("X0", _.X0, (a, b) => a.X0 = b) +=
        createElement("Y0", _.Y0, (a, b) => a.Y0 = b) +=
        createElement("Z0", _.Z0, (a, b) => a.Z0 = b) +=
        createElement("X1", _.X1, (a, b) => a.X1 = b) +=
        createElement("Y1", _.Y1, (a, b) => a.Y1 = b) +=
        createElement("X2", _.X2, (a, b) => a.X2 = b) +=
        createElement("TX0", _.TX0, (a, b) => a.TX0 = b) +=
        createElement("TY0", _.TY0, (a, b) => a.TY0 = b) +=
      createElement("TZ0", _.TZ0, (a, b) => a.TZ0 = b) +=
      createElement("α*", _.alphaMul, (a, b) => a.alphaMul = b)
    subElements ++= allAdjusters


    override def layout() {
        super.layout()
        val tW = width / 3
        val tH = height / 4
        for ((ae, i) <- allAdjusters.view.zipWithIndex) {
            ae.setSize(tW, tH)
            ae.posX = (i % 3) * tW
            ae.posY = (i / 3) * tH
        }
    }
}
