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
package moe.nightfall.instrumentality.editor;

import moe.nightfall.instrumentality.Loader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

/**
 * Created on 03/09/15.
 */
public class UISystemFont {
    // We're /NOT/ uploading this to the GPU every frame just to copy it back down again as a UI draw.
    private static HashMap<String, TextTexture> textTextures = new HashMap<String, TextTexture>();
    // Used to avoid textTextures getting too big
    // (this is a ring, freeTextPtr goes around the ring as textures are alloc'd,
    //  when it runs into old text, it deletes it)
    private static String[] freeTextRing = new String[512];
    private static int freeTextPtr = 0;

    // Scratch buffer for font rendering (To measure how big the rendered text will be, we have to measure it first.)
    private static BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
    private static Graphics fontTestRender = bi.getGraphics();

    /**
     * Private since this should NEVER be called when we can safely use the "nice" text.
     * We don't want to discriminate against other languages, but we don't want to bludgeon appearance
     * (and potentially compatibility - I consider font rendering a black box, you put text in and you get pixels out)
     * because of (language) support, for better or worse.
     * The vertical size of one line should be 9u(opengl units) - the horizontal size is UNBOUNDED!
     *
     * @param str The text you want to display.
     * @return The resulting size.
     */
    public static Vector2f drawSystemLine(String str, Font targetFont) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        TextTexture tex = null;
        if (textTextures.containsKey(str)) {
            tex = textTextures.get(str);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.tex);
        } else {
            if (freeTextRing[freeTextPtr] != null) {
                tex = textTextures.get(freeTextRing[freeTextPtr]);
                textTextures.remove(freeTextRing[freeTextPtr]);
                // reuse the texture object and hope for the best
            } else {
                tex = new TextTexture();
                tex.tex = GL11.glGenTextures();
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.tex);
            textTextures.put(str, tex);
            freeTextRing[freeTextPtr++] = str;
            freeTextPtr = freeTextPtr % freeTextRing.length;
            fontTestRender.setFont(targetFont);
            FontMetrics fm = fontTestRender.getFontMetrics();
            // Work out what we need
            Rectangle2D neededSize = fm.getStringBounds(str, fontTestRender);
            BufferedImage mainImage = new BufferedImage((int) (neededSize.getWidth() + 1), (int) (neededSize.getHeight() + 1), BufferedImage.TYPE_INT_ARGB);
            Graphics g = mainImage.createGraphics();
            g.setFont(targetFont);
            g.setColor(Color.white);
            g.drawString(str, (int) -neededSize.getX(), (int) -neededSize.getY());
            g.dispose();
            Loader.writeGLTexImg(mainImage, GL11.GL_LINEAR);
            tex.w = mainImage.getWidth();
            tex.h = mainImage.getHeight();

            // Ok, now get a 1-liner's metrics so we can keep everything sane in proportion to our own text engine
            Rectangle2D oneLinerSize = fm.getStringBounds("|_`[]{}右つま先ＩＫ先", fontTestRender);
            tex.scale = 9d / oneLinerSize.getHeight();
        }
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_LINEAR, GL11.GL_LINEAR);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor3d(1, 1, 1);

        GL11.glVertex3d(0, 0, 0);
        GL11.glTexCoord2d(0, 1);

        GL11.glVertex3d(0, tex.h * tex.scale, 0);
        GL11.glTexCoord2d(1, 1);

        GL11.glVertex3d(tex.w * tex.scale, tex.h * tex.scale, 0);
        GL11.glTexCoord2d(1, 0);

        GL11.glVertex3d(tex.w * tex.scale, 0, 0);
        GL11.glTexCoord2d(0, 0);

        GL11.glEnd();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        return new Vector2f((float) (tex.w * tex.scale), (float) (tex.h * tex.scale));
    }

    private static class TextTexture {
        public int tex, w, h;
        double scale; // scale needed to adjust w/h into normal units
    }
}
