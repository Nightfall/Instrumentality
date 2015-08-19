package moe.nightfall.instrumentality.editor;

import moe.nightfall.instrumentality.Main;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/**
 * Created on 18/08/15.
 */
public class EditElement {
    // Note that posX and posY may be ignored if this is the root
    private int sizeWidth, sizeHeight;
    public int posX, posY;

    protected void drawRect(int x, int y, int w, int h, double r, double g, double b) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4d(r, g, b, 1);
        GL11.glVertex3d(x, y, 0);
        GL11.glVertex3d(x, y + h, 0);
        GL11.glVertex3d(x + w, y + h, 0);
        GL11.glVertex3d(x + w, y, 0);
        GL11.glEnd();
    }

    protected void drawSkinnedRect(int x, int y, int w, int h, double strength) {
        double str = 1.0f;
        double step = (1.0f - strength) / 4;
        for (int i = 0; i < 4; i++) {
            drawRect(x + i, y + i, w - (i * 2), h - (i * 2), 0, str * 0.5f, str);
            str -= step;
        }
    }

    public void draw() {
        drawSkinnedRect(posX, posY, sizeWidth, sizeHeight, 0.5f);
    }

    public void setSize(int width, int height) {
        sizeWidth = width;
        sizeHeight = height;
    }

    public int getWidth() {
        return sizeWidth;
    }

    public int getHeight() {
        return sizeHeight;
    }

    public void cleanup() {

    }
}
