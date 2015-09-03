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

/**
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
    public static String[] fontDB = {
            "0",
            "SSB",
            "  0 1  ",
            "     2 ",
            "       ",
            "      3",
            "       ",
            "9     4",
            "       ",
            " 8   5 ",
            "  7 6  ",

            "  3    ",
            " 2     ",
            "       ",
            "1      ",
            "       ",
            "0      ",
            "       ",
            "       ",
            "       ",

            "       ",
            "       ",
            "       ",
            "   1   ",
            "       ",
            "   0   ",
            "       ",
            "       ",
            "       ",

            "1",
            "SB",
            "  12   ",
            "0      ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "   3   ",
            "6 7 4 5",

            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "   1   ",
            "  0    ",

            "2",
            "S",
            "  1 2  ",
            "0     3",
            "       ",
            "       ",
            "    4  ",
            "  5    ",
            "       ",
            "6      ",
            "  7   8",

            "3",
            "SS",
            "  2 3  ",
            "1     4",
            "       ",
            "      5",
            "  7 6  ",
            "       ",
            "       ",
            "       ",
            "       ",

            "       ",
            "       ",
            "       ",
            "       ",
            "    0  ",
            "      1",
            "       ",
            "5     2",
            "  4 3  ",

            "4",
            "SB",
            "    1  ",
            "       ",
            "       ",
            "       ",
            "       ",
            "0      ",
            "       ",
            "       ",
            "    2  ",

            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "0     1",
            "       ",
            "       ",
            "       ",

            "5",
            "S",
            "1     0",
            "       ",
            "       ",
            "  3 4  ",
            "2      ",
            "      5",
            "      6",
            "9      ",
            "  8 7  ",

            "6",
            "SS",
            "  2 1  ",
            "3     0",
            "       ",
            "       ",
            "4     9",
            "       ",
            "       ",
            "5     8",
            "  6 7  ",

            "       ",
            "       ",
            "       ",
            "  2 1  ",
            "3     0",
            "       ",
            "       ",
            "       ",
            "       ",

            "7",
            "S",
            "0    1 ",
            "      2",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "3      ",

            "8",
            "LS",
            "  1 2  ",
            "0     3",
            "       ",
            "7     4",
            "  6 5  ",
            "       ",
            "       ",
            "       ",
            "       ",

            "       ",
            "       ",
            "       ",
            "       ",
            "  0 7  ",
            "1     6",
            "       ",
            "2     5",
            "  3 4  ",

            "9",
            "LS",
            "  1 2  ",
            "0     3",
            "       ",
            "7     4",
            "  6 5  ",
            "       ",
            "       ",
            "       ",
            "       ",

            "       ",
            "       ",
            "       ",
            "      0",
            "       ",
            "      1",
            "       ",
            "     2 ",
            "   3   ",

// lowercase a-z

            "aA",
            "LS",
            "       ",
            "       ",
            "       ",
            "       ",
            "  6 5  ",
            "7     4",
            "       ",
            "0     3",
            "  1 2  ",

            "       ",
            "       ",
            "  3 2  ",
            "4     1",
            "       ",
            "      0",
            "       ",
            "       ",
            "       ",

            "bB",
            "S",
            "0      ",
            "       ",
            "       ",
            "7   6  ",
            "       ",
            "      5",
            "1     4",
            "       ",
            "  2 3  ",

            "cC",
            "S",
            "       ",
            "       ",
            "  1   0",
            "       ",
            "2      ",
            "       ",
            "3      ",
            "       ",
            "  4   5",

            "dD",
            "S",
            "      0",
            "       ",
            "       ",
            "  6   7",
            "       ",
            "5      ",
            "4     1",
            "       ",
            "  3 2  ",

            "eE",
            "LS",
            "       ",
            "       ",
            "  0 1  ",
            "7     2",
            "6     3",
            "  5 4  ",
            "       ",
            "       ",
            "       ",

            "       ",
            "       ",
            "       ",
            "       ",
            "0      ",
            "       ",
            "1      ",
            " 2     ",
            "      3",

            "fF",
            "SB",
            "      3",
            "   2   ",
            "       ",
            "  1    ",
            "       ",
            "       ",
            "       ",
            "       ",
            "  0    ",

            "       ",
            "       ",
            "       ",
            "0     1",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",

            "gG",
            "LS",
            "       ",
            "       ",
            " 0   1 ",
            "7     2",
            "6     3",
            " 5   4 ",
            "       ",
            "       ",
            "       ",

            "       ",
            "       ",
            "       ",
            "       ",
            "      0",
            "      1",
            "       ",
            "     2 ",
            "4  3   ",

            "hH",
            "BS",
            "0      ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "1      ",

            "       ",
            "       ",
            "       ",
            "  1 2  ",
            "0     3",
            "       ",
            "       ",
            "       ",
            "      4",

            "i",
            "B",
            "       ",
            "  0 1  ",
            "       ",
            "   2   ",
            "       ",
            "       ",
            "       ",
            "       ",
            "   3   ",

            "jJ",
            "BS",
            "    0 1",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",

            "       ",
            "       ",
            "     0 ",
            "       ",
            "       ",
            "     1 ",
            "       ",
            "    2  ",
            "4 3    ",

            "kK",
            "BB",
            "       ",
            "       ",
            "      5",
            "       ",
            "       ",
            "4      ",
            "       ",
            "       ",
            "       ",

            "       ",
            "       ",
            "0      ",
            "       ",
            "       ",
            "2      ",
            "       ",
            "       ",
            "1     3",

            "lL",
            "S",
            "       ",
            "0      ",
            "       ",
            "       ",
            "       ",
            "       ",
            "1      ",
            " 2     ",
            "   3  4",

            "m",
            "SS",
            "       ",
            "       ",
            "  2    ",
            "1  3   ",
            "       ",
            "       ",
            "       ",
            "   4   ",
            "0      ",

            "       ",
            "       ",
            "    1  ",
            "   0  2",
            "       ",
            "       ",
            "       ",
            "       ",
            "      3",

            "nN",
            "S",
            "       ",
            "       ",
            "  2 3  ",
            "1     4",
            "       ",
            "       ",
            "       ",
            "       ",
            "0     5",

            "oO",
            "L",
            "       ",
            "       ",
            " 9   8 ",
            "0     7",
            "       ",
            "       ",
            "       ",
            "1     6",
            " 2   5 ",

            "pP",
            "S",
            "       ",
            "       ",
            " 2   3 ",
            "1     4",
            "      5",
            "7    6 ",
            "       ",
            "       ",
            "0      ",

            "qQ",
            "S",
            "       ",
            "       ",
            " 3   2 ",
            "4     1",
            "5      ",
            " 6    7",
            "       ",
            "       ",
            "      0",

            "rR",
            "S",

            "       ",
            "       ",
            "  2   3",
            "       ",
            "1      ",
            "       ",
            "       ",
            "       ",
            "0      ",

            "sS",
            "S",
            "       ",
            "       ",
            "  2 1  ",
            "3     0",
            "4      ",
            "       ",
            "      5",
            "9     6",
            "  8 7  ",

            "tT",
            "SB",
            "   0   ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "   1   ",
            "    2  ",
            "      3",

            "       ",
            "       ",
            "0     1",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",

            "uU",
            "S",
            "       ",
            "       ",
            "0     7",
            "       ",
            "       ",
            "       ",
            "       ",
            "1     6",
            "  2 5  ",

            "vV",
            "S",
            "       ",
            "       ",
            "0     4",
            "       ",
            " 1   3 ",
            "       ",
            "       ",
            "       ",
            "   2   ",

            "wW",
            "S",
            "       ",
            "       ",
            "0     4",
            "   2   ",
            "       ",
            "       ",
            "       ",
            "       ",
            " 1   3 ",

            "xX",
            "B",
            "       ",
            "       ",
            "0     3",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "2     1",

            "yY",
            "SS",
            "       ",
            "      3",
            "       ",
            "       ",
            "       ",
            "       ",
            "     2 ",
            "    1  ",
            "0      ",

            "       ",
            "  0    ",
            "       ",
            "       ",
            "       ",
            "       ",
            "     1 ",
            "       ",
            "       ",

            "zZ",
            "S",
            "       ",
            "       ",
            "0    1 ",
            "      2",
            "       ",
            "       ",
            "       ",
            "3      ",
            " 4    5",
// CAPITAL A-Z

            "I",
            "B",
            "0  2  1",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "4  3  5",

            "M",
            "SS",
            "  2    ",
            "1  3   ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "   4   ",
            "0      ",

            "    1  ",
            "   0  2",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "       ",
            "      3",
    };
    public static int[] cachedFont = new int[256];

    public static void drawChar(char c) {
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
        int p = 0;
        while (p < fontDB.length) {
            String activeChars = fontDB[p++];
            char[] passes = fontDB[p++].toCharArray();
            String[] passData = new String[passes.length * 9];
            for (int i = 0; i < passData.length; i++)
                passData[i] = fontDB[p++];
            if (activeChars.contains(String.valueOf(c))) {
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
                    for (int thick = 3; thick >= 0; thick--) {
                        GL11.glColor3d(0, thick / 8d, thick / 5d);
                        GL11.glLineWidth(thick + 1);
                        GL11.glBegin(t);
                        for (int n = 0; n < 10; n++)
                            if (isUsed[n])
                                GL11.glVertex2d(x[n], y[n]);
                        if (type == 'L')
                            GL11.glVertex2d(x[0], y[0]);
                        GL11.glEnd();
                    }
                }
                if (isCompiling)
                    GL11.glEndList();
                return;
            }
        }
        if (isCompiling)
            GL11.glEndList();
    }
}
