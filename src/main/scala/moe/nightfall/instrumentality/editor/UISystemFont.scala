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
package moe.nightfall.instrumentality.editor

;

import java.awt._
import java.awt.image.BufferedImage

import moe.nightfall.instrumentality.Loader
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f;

/**
 * Created on 03/09/15.
 */
object UISystemFont {
    // We're /NOT/ uploading this to the GPU every frame just to copy it back down again as a UI draw.
    private var textTextures = scala.collection.mutable.HashMap[String, TextTexture]()

    // Used to avoid textTextures getting too big
    // (this is a ring, freeTextPtr goes around the ring as textures are alloc'd,
    //  when it runs into old text, it deletes it)
    private val freeTextRing = new Array[String](32)
    private var freeTextPtr = 0

    // Scratch buffer for font rendering (To measure how big the rendered text will be, we have to measure it first.)
    private val bi = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY)
    private val fontTestRender = bi.getGraphics()

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
    def drawSystemLine(str: String, targetFont: Font): Vector2f = {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        var tex: TextTexture = null
        if (textTextures.contains(str)) {
            tex = textTextures.get(str).get
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.tex)
        } else {
            if (freeTextRing(freeTextPtr) != null) {
                tex = textTextures.get(freeTextRing(freeTextPtr)).get
                textTextures.remove(freeTextRing(freeTextPtr))
                // reuse the texture object and hope for the best
            } else {
                tex = new TextTexture()
                tex.tex = GL11.glGenTextures()
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.tex)
            textTextures.put(str, tex)
            freeTextRing(freeTextPtr) = str
            freeTextPtr += 1
            freeTextPtr = freeTextPtr % freeTextRing.length
            // Work out what we need
            val neededSize = sizeSystemLine(str, targetFont)
            val mainImage = new BufferedImage(neededSize._1.x.toInt, neededSize._1.x.toInt, BufferedImage.TYPE_INT_ARGB)
            val g = mainImage.createGraphics()
            g.setFont(targetFont)
            g.setColor(Color.BLACK)
            g.drawString(str, -neededSize._2.x.toInt, -neededSize._2.y.toInt)
            g.dispose()
            Loader.writeGLTexImg(mainImage, GL11.GL_LINEAR)
            tex.w = mainImage.getWidth()
            tex.h = mainImage.getHeight()

            tex.scale = neededSize._3
        }
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glColor3d(1, 1, 1)

        GL11.glTexCoord2d(0, 0)
        GL11.glVertex3d(0, 0, 0)

        GL11.glTexCoord2d(0, 1)
        GL11.glVertex3d(0, tex.h * tex.scale, 0)

        GL11.glTexCoord2d(1, 1)
        GL11.glVertex3d(tex.w * tex.scale, tex.h * tex.scale, 0)

        GL11.glTexCoord2d(1, 0)
        GL11.glVertex3d(tex.w * tex.scale, 0, 0)

        GL11.glEnd()
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        return new Vector2f((tex.w * tex.scale).toFloat, (tex.h * tex.scale).toFloat)
    }

    // The Vector2f is the size in pixels, the second v2f is the offset in pixels, the double is the scale relative to the original size.
    // (The second v2f can be ignored outside of here,
    // but if you *want* to draw your text with the offsets, then feel free to.)
    // To get the size in GL units, just multiply the first v2f by the scale.
    def sizeSystemLine(str: String, targetFont: Font): (Vector2f, Vector2f, Double) = {
        fontTestRender.setFont(targetFont)
        val fm = fontTestRender.getFontMetrics()
        // Work out what we need
        val neededSize = fm.getStringBounds(str, fontTestRender)
        val scale = 9d / fm.getHeight()
        (new Vector2f(neededSize.getWidth.toFloat + 1, neededSize.getHeight.toFloat + 1), new Vector2f(neededSize.getX.toFloat, neededSize.getY.toFloat), scale)
    }

    /**
     * Fonts are lazy-loaded, causing HUGE amounts of lag for the poor user.
     * This triggers some functions to try and get the font to be loaded... on a different thread!
     */
    def scratchFont(f: Font) {
        fontTestRender.setFont(f)
        val fm = fontTestRender.getFontMetrics()
        fm.getStringBounds("ATTEMPT TO CAUSE LAG ON A DIFFERENT THREAD", fontTestRender)
        fontTestRender.drawString("Carried away by a moonlight shadow...", 0, 0)
    }

    private class TextTexture {
        var tex, w, h: Int = _
        var scale: Double = _ // scale needed to adjust w/h into normal units
    }

}
