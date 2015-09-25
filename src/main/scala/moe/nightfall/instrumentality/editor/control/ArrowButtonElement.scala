package moe.nightfall.instrumentality.editor.control

import org.lwjgl.opengl.GL11


class ArrowButtonElement(arrowAngle: Double, toRun: () => Unit) extends ButtonElement(toRun) {
    def this(arrowAngle: Double, toRun: Runnable) = this(arrowAngle, () => toRun.run())

    override def draw(scrWidth: Int, scrHeight: Int) {
        super.draw(scrWidth, scrHeight)
        val w: Double = 1 / 8.0
        val h: Double = 1 / 8.0
        val wu = 3
        val hu = 3

        GL11.glPushMatrix()
        GL11.glTranslated(width * w * 4, width * h * 4, 0)
        GL11.glScaled(width, height, 1)
        GL11.glRotated(arrowAngle, 0, 0, 1)
        GL11.glColor3d(0, 0, 0)
        GL11.glBegin(GL11.GL_TRIANGLES)
        GL11.glVertex2d(-(w * wu), -(h * hu))
        GL11.glVertex2d(w * (wu - 1), 0)
        GL11.glVertex2d(w * wu, 0)
        GL11.glVertex2d(-(w * wu), h * hu)
        GL11.glVertex2d(w * wu, 0)
        GL11.glVertex2d(w * (wu - 1), 0)
        GL11.glEnd()
        GL11.glPopMatrix()
    }
}
