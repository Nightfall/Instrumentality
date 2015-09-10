package moe.nightfall.instrumentality.editor.controls;

import org.lwjgl.opengl.GL11;

import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.UIUtils;

public class LabelElement extends EditElement {
    public String theText;

    public LabelElement(String txt) {
        theText = txt;
    }

    @Override
    public void draw(int scrWidth, int scrHeight) {
        super.draw(scrWidth, scrHeight);
        GL11.glPushMatrix();
        double scale = (getHeight() - borderWidth) / 8d;
        if (scale < 1.7) {
            scale = getHeight() / 8d;
        } else {
            GL11.glTranslated(borderWidth/2, borderWidth/2, 0);
        }
        GL11.glScaled(scale, scale, 1);
        UIUtils.drawText(theText, 2);
        GL11.glPopMatrix();
    }
}
