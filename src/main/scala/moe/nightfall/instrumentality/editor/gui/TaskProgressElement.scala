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

import moe.nightfall.instrumentality.{TaskState, MeasurableTask}
import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control.{ProgressBarElement, LabelElement, PowerlineContainerElement}
import org.lwjgl.opengl.GL11

/**
 * For long-running tasks.
 * Created on 26/09/15.
 */
class TaskProgressElement(val rootPanel: PowerlineContainerElement, val preempted: EditElement, val noCleanupPreempted: Boolean, val task: MeasurableTask) extends EditElement {
    var returnPlease = false
    var tasksOnReturn = Seq[(TaskProgressElement) => Unit]()

    def addReturnTask(function: (TaskProgressElement) => Unit) = {
        tasksOnReturn :+= function
    }

    val label = new LabelElement("Following the indeterminable brightness-rabbit...")
    val progressBar = new ProgressBarElement()

    subElements += label
    subElements += progressBar

    def performReturn() = {
        // done here so stuff that would be done on layout will work
        tasksOnReturn.foreach(_(this))
        rootPanel.setUnderPanel(preempted, noCleanupPreempted)
    }

    override def draw() = {
        // This does entirely custom draw logic!
        preempted.draw()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        drawRect(0, 0, width, height, 0.1f, 0.1f, 0.1f, 0.5f)
        GL11.glDisable(GL11.GL_BLEND)
        drawSubelements()
    }

    override def layout() = {
        preempted.setSize(width, height)
        label.posX = width / 16
        label.posY = height / 32
        label.setSize(width - (width / 8), (height / 2) - (height / 16))

        progressBar.posX = width / 16
        progressBar.posY = height / 2
        progressBar.setSize(width - (width / 8), height / 8)
    }

    override def update(time: Double) = {
        super.update(time)
        label.text = task.generalTask + "\r\n" + task.subTask
        progressBar.progressValue = task.progress
        if (task.state == TaskState.Success)
            returnPlease = true
        if (returnPlease)
            performReturn()
    }
}
