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

import moe.nightfall.instrumentality.{ModelCache, Loader}
import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control._

/**
 * Created on 25/08/15, ported to Scala on 2015-09-20
 */
class ModelChooserElement(val availableModels: Seq[String], powerlineContainerElement: PowerlineContainerElement) extends EditElement {
    private var group = Array.fill[ModelElement](3)(new ModelElement(true))
    subElements ++= group

    private var ptrStart: Int = 0
    private var x22H: BoxElement = new BoxElement(false)
    private var x22VA: BoxElement = new BoxElement(true)
    private var x22VB: BoxElement = new BoxElement(true)
    x22H += x22VA
    x22H += x22VB
    x22VA += new TextButtonElement("Animations", {
        val mdl = ModelCache.getLocal(Loader.currentFile)
        if (mdl != null) {
            val l = new PoseTreeElement(mdl, powerlineContainerElement)
            powerlineContainerElement.addAndGo("Animations", l)
        }
    })
    x22VA += new TextButtonElement("Benchmark", {
        val mdl = ModelCache.getLocal(Loader.currentFile)
        if (mdl != null) {
            val l = new BenchmarkElement(mdl)
            powerlineContainerElement.addAndGo("Benchmark", l)
        }
    })
    x22VB += new TextButtonElement("Settings", {
        //        val l=new DownloaderElement(powerlineContainerElement)
        //        powerlineContainerElement.addAndGo("Downloader",l)
    })
    x22VB += new TextButtonElement("Downloader", {
        val l = new DownloaderElement(powerlineContainerElement)
        powerlineContainerElement.addAndGo("PMX Downloader", l)
    })
    private var buttonbar = Array[EditElement](
        new ArrowButtonElement(180, {
            ptrStart -= 1
            if (ptrStart < 0)
                ptrStart = availableModels.length
            updatePosition()
        }),
        new ArrowButtonElement(0, {
            ptrStart += 1
            updatePosition()
        }),
        new ModelElement(false),
        x22H
    )
    subElements ++= buttonbar
    updatePosition()

    override def layout() = {
        val x = width / group.length
        val y = height / 4

        for ((element, index) <- group.view.zipWithIndex) {
            element.posX = x * index
            element.posY = y
            element.setSize(x, y * 3)
        }

        for ((button, index) <- buttonbar.view.zipWithIndex) {
            button.posX = y * index
            button.posY = 0
            button.setSize(if (index != buttonbar.length - 1) y else width - (y * index), y)
        }
    }

    def updatePosition() = {
        for ((element, index) <- group.view.zipWithIndex) {
            val indi = (index + ptrStart) % (availableModels.length + 1)
            if (indi == 0) {
                element.setModel(null)
            } else {
                element.setModel(availableModels(indi - 1))
            }
        }
    }
}
