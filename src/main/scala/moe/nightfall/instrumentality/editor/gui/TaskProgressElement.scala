package moe.nightfall.instrumentality.editor.gui

import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control.{LabelElement, ButtonBarContainerElement}
import org.lwjgl.opengl.GL11

/**
 * For long-running tasks.
 * Created on 26/09/15.
 */
class TaskProgressElement(val rootPanel: ButtonBarContainerElement, val preempted: EditElement, val noCleanupPreempted: Boolean) extends EditElement {

    var returnPlease = false
    var tasksOnReturn = Seq[(TaskProgressElement) => Unit]()

    def addReturnTask(function: (TaskProgressElement) => Unit) = {
        tasksOnReturn :+= function
    }

    val label = new LabelElement("Following the indeterminable brightness-rabbit...")
    subElements += label

    def performReturn() = {
        rootPanel.setUnderPanel(preempted, noCleanupPreempted)
        tasksOnReturn.foreach(_(this))
    }

    override def draw(scrwidth: Int, scrheight: Int) = {
        // This does entirely custom draw logic!
        preempted.draw(scrwidth, scrheight)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        drawRect(0, 0, width, height, 0.1, 0.1, 0.1, 0.5)
        GL11.glDisable(GL11.GL_BLEND)
        drawSubelements(scrwidth, scrheight)
    }

    override def layout() = {
        preempted.setSize(width, height)
        label.posX = width / 16
        label.posY = height / 32
        label.setSize(width - (width / 8), height / 16)
    }

    override def update(time: Double) = {
        super.update(time)
        if (returnPlease)
            performReturn()
    }
}
