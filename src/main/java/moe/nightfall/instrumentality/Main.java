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
package moe.nightfall.instrumentality;

import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.UIUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * FULL LIST OF KEYBOARD CONTROLS: WASD: Camera controls. FV: Zoom control! E:
 * Animation Update Toggle. R: Walking Flag Disable. Up/Down: Turns on/off the
 * walking flag. Left/Right: Time Controls On Unit 00. Shift: Sneak Ctrl: Sprint
 * C(obalt) Q: YOU NEED THE CONSOLE FOR THIS: Lists emotes. Type in the name to
 * apply it.
 * <p/>
 * NOTE: To use the following you need to actually modify the code in places
 * <p/>
 * TYUIO,GHJKL: Controlling some parameters is difficult so this allows live
 * feedback Enter: Dumps live feedback data
 * <p/>
 * These controls exist to be used when working on Emote poses. It simplifies
 * the process quite a bit :)
 * <p/>
 * Before using this code, look in PlayerControlAnimation for some notes
 * <p/>
 * Created on 24/07/15.
 */
public class Main {

    private EditElement currentPanel;

    public static void main(String[] args) throws Exception {
        Main m = new Main();
        ModelCache.getLocalModels();
        m.startWorkbench();
    }

    public void startWorkbench() throws Exception {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

        int scrWidth = 800, scrHeight = 600;
        Display.setTitle("Instrumentality: PMX Animation Workbench");
        Display.setDisplayMode(new DisplayMode(scrWidth, scrHeight));
        Display.setResizable(true);
        Display.create();
        Mouse.create();

        Loader.setup();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        long frameEndpoint = System.currentTimeMillis();

        Keyboard.create();
        double deltaTime = 0.1;

        onSizeChange(scrWidth, scrHeight);

        changePanel(UIUtils.createGui());

        while (!Display.isCloseRequested()) {
            if (Display.wasResized()) {
                scrWidth = Display.getWidth();
                scrHeight = Display.getHeight();
                onSizeChange(scrWidth, scrHeight);
            }
            long frameStart = System.currentTimeMillis();
            GL11.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            GL11.glClearDepth(1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glLoadIdentity();

            Keyboard.poll();
            Mouse.poll();

            doUpdate(deltaTime);

            doDraw();

            Display.update();

            long currentTime = System.currentTimeMillis();
            // Frame start is frameEndpoint-20 (note that the delta does include
            // the sleep)
            int delta = (int) (currentTime - (frameEndpoint - 30));
            deltaTime = delta / 1000.0d;

            long v = frameEndpoint - currentTime;
            if (v > 1)
                Thread.sleep(v);
            Display.setTitle("I-PMXAW: FrameTime " + (currentTime - frameStart) + "ms");
            frameEndpoint = currentTime + 30;
        }
        Display.destroy();
    }

    private void onSizeChange(int scrWidth, int scrHeight) {
        GL11.glViewport(0, 0, scrWidth, scrHeight);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, scrWidth, scrHeight, 0, 0, 1024);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        if (currentPanel != null)
            currentPanel.setSize(scrWidth, scrHeight);
    }

    private void doUpdate(double dT) {
        UIUtils.update(currentPanel);
        currentPanel.update(dT);
    }

    private void doDraw() {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        currentPanel.draw(Display.getWidth(), Display.getHeight());
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    public void changePanel(EditElement newPanel) {
        currentPanel = newPanel;
        currentPanel.setSize(Display.getWidth(), Display.getHeight());
    }
}
