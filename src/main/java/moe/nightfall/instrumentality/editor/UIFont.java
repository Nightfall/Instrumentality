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
public class UIFont {
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
    public static String[] fontDB;

    public static void setFont(InputStream inp) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inp));
        LinkedList<String> l = new LinkedList<String>();
        while (br.ready()) {
            String s = br.readLine();
            if (s.endsWith(";"))
                l.add(s.substring(0, s.length() - 1));
        }
        String[] fontData = new String[l.size()];
        int i = 0;
        for (String s : l)
            fontData[i++] = s;
        fontDB = fontData;
    }

    public static int[] cachedFont = new int[256];

    protected static void drawChar(char c) {
        // bit of a hypocrite here, rendering each char individually, but strings are at a higher layer.
        // hopefully the 1 array lookup and 1 call-list is fast enough?
        boolean isCompiling = false;
        if (c < cachedFont.length) {
            if (cachedFont[c] != 0) {
                GL11.glCallList(cachedFont[c]);
                return;
            } else {
                int lst = GL11.glGenLists(1);
                GL11.glNewList(lst, GL11.GL_COMPILE_AND_EXECUTE);
                cachedFont[c] = lst;
                isCompiling = true;
            }
        }
        int p = getCharLoc(c);
        if (p == -1) {
            if (isCompiling)
                GL11.glEndList();
            return;
        }
        char[] passes = fontDB[p++].toCharArray();
        String[] passData = new String[passes.length * 9];
        for (int i = 0; i < passData.length; i++)
            passData[i] = fontDB[p++];
        int passPos = 0;
        for (int i = 0; i < passes.length; i++) {
            char type = passes[i];
            int t = GL11.GL_LINE_STRIP;
            if (type == 'B')
                t = GL11.GL_LINES;
            double[] x = new double[10];
            double[] y = new double[10];
            boolean[] isUsed = new boolean[10];
            for (int j = 0; j < 9; j++) {
                String str = passData[passPos++];
                for (int n = 0; n < 10; n++)
                    if (str.contains(String.valueOf((char) ('0' + n)))) {
                        x[n] = str.indexOf(String.valueOf((char) ('0' + n)));
                        y[n] = j;
                        isUsed[n] = true;
                    }
            }
            GL11.glBegin(t);
            for (int n = 0; n < 10; n++)
                if (isUsed[n])
                    GL11.glVertex2d(x[n], y[n]);
            if (type == 'L')
                GL11.glVertex2d(x[0], y[0]);
            GL11.glEnd();
        }
        if (isCompiling)
            GL11.glEndList();
    }

    private static int[] cachedPositions = new int[256];

    public static int getCharLoc(char c) {
        boolean canCache = c < 256;
        if (canCache)
            if (cachedPositions[(int) c] != 0)
                return cachedPositions[(int) c];
        int p = 0;
        while (p < fontDB.length) {
            String activeChars = fontDB[p++];
            if (activeChars.contains(String.valueOf(c))) {
                if (canCache)
                    cachedPositions[(int) c] = p;
                return p;
            }
            String passes = fontDB[p++];
            p += passes.length() * 9;
        }
        if (canCache)
            cachedPositions[(int) c] = -1;
        return -1;
    }
}
