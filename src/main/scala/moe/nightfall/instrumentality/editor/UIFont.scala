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

import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

/**
 * A simple no-fuss vector font renderer.
 * Created on 02/09/15.
 */
object UIFont {
    /*
     * +-----
     * | 0   1
     * |      2
     * |
     * |      3
     * | 5   4
     * |6
     * |
     * |7
     * | 8   9
     * curves can be L(oop) S(trip) or B(asic GL_LINES)
     */
    var fontDB : Seq[String] = _

    def setFont(inp : InputStream) {
        val br = new BufferedReader(new InputStreamReader(inp))
        fontDB = Iterator.continually {
            val s = br.readLine()
            if (s.endsWith(";"))
                Some(s.substring(0, s.length() - 1))
            else None    
        }.takeWhile(_ => br.ready).flatten.toSeq
        
        // TODO SCALA Test this before removal
       /* LinkedList<String> l = new LinkedList<String>();
        while (br.ready()) {
            String s = br.readLine();
            if (s.endsWith(";"))
                l.add();
        }
        String[] fontData = new String[l.size()];
        int i = 0;
        for (String s : l)
            fontData[i++] = s;
        fontDB = fontData; */
    }

    val cachedFont = new Array[Int](256)

    def drawChar(c : Char) {
        // bit of a hypocrite here, rendering each char individually, but strings are at a higher layer.
        // hopefully the 1 array lookup and 1 call-list is fast enough?
        var isCompiling = false
        if (c < cachedFont.length) {
            if (cachedFont(c) != 0) {
                GL11.glCallList(cachedFont(c))
                return
            } else {
                val lst = GL11.glGenLists(1)
                GL11.glNewList(lst, GL11.GL_COMPILE_AND_EXECUTE)
                cachedFont(c) = lst
                isCompiling = true
            }
        }
        var p = getCharLoc(c)
        if (p == -1) {
            if (isCompiling)
                GL11.glEndList()
            return
        }
        val passes = fontDB(p).toCharArray()
        p += 1
        val passData = new Array[String](passes.length * 9)
        for (i <- 0 until passData.length) {
            passData(i) = fontDB(p)
            p += 1
        }
        var passPos = 0
        for (i <- 0 until passes.length) {
            val tpe = passes(i)
            var t = GL11.GL_LINE_STRIP
            if (tpe == 'B')
                t = GL11.GL_LINES
            var x = new Array[Double](10)
            var y = new Array[Double](10)
            var isUsed = new Array[Boolean](10)
            for (j <- 0 to 10) {     
                val str = passData(passPos)
                passPos += 1
                for (n <- 0 until 10) {
                    if (str.contains(String.valueOf(('0' + n).toChar))) {
                        x(n) = str.indexOf(String.valueOf(('0' + n).toChar))
                        y(n) = j
                        isUsed(n) = true
                    }
                }
            }
            GL11.glBegin(t)
            for (n <- 0 until 10)
                if (isUsed(n))
                    GL11.glVertex2d(x(n), y(n));
            if (tpe == 'L')
                GL11.glVertex2d(x(0), y(0))
            GL11.glEnd()
        }
        if (isCompiling)
            GL11.glEndList()
    }

    val cachedPositions = new Array[Int](256)

    def getCharLoc(c : Char) : Int = {
        val canCache = c < 256
        if (canCache)
            if (cachedPositions(c) != 0)
                return cachedPositions(c)
        var p = 0
        while (p < fontDB.length) {
            val activeChars = fontDB(p)
            p += 1
            if (activeChars.contains(String.valueOf(c))) {
                if (canCache)
                    cachedPositions(c) = p
                return p
            }
            val passes = fontDB(p)
            p += 1
            p += passes.length() * 9;
        }
        if (canCache)
            cachedPositions(c) = -1
        return -1
    }
}
