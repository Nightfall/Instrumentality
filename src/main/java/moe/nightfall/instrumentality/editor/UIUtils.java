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
import moe.nightfall.instrumentality.ModelCache;
import moe.nightfall.instrumentality.editor.controls.ButtonBarContainerElement;
import moe.nightfall.instrumentality.editor.controls.ButtonElement;
import moe.nightfall.instrumentality.editor.guis.BenchmarkElement;
import moe.nightfall.instrumentality.editor.guis.ModelChooserElement;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public class UIUtils {
    public static boolean[] state=new boolean[2];
    public static void update(EditElement targetPanel) {
        boolean[] newState = new boolean[2];
        newState[0] = Mouse.isButtonDown(0);
        newState[1] = Mouse.isButtonDown(1);
        int x = Mouse.getX();
        int y = Display.getHeight() - (Mouse.getY() + 1);
        if (newState[0] != state[0])
            targetPanel.mouseStateChange(x, y, newState[0], false);
        if (newState[1] != state[1])
            targetPanel.mouseStateChange(x, y, newState[1], true);
        targetPanel.mouseMove(x, y, newState);
        state = newState;
    }

    public static EditElement createGui() {
        final ButtonBarContainerElement bbce = new ButtonBarContainerElement(0.05d);

        final ModelChooserElement mce = new ModelChooserElement(ModelCache.getLocalModels());
        bbce.setUnderPanel(mce);

        bbce.barCore.addPiece(new ButtonElement(new Runnable() {
            @Override
            public void run() {
                bbce.setUnderPanel(mce);
            }
        }));

        bbce.barCore.addPiece(new ButtonElement(new Runnable() {
            @Override
            public void run() {
                if (Loader.currentFile != null)
                    bbce.setUnderPanel(new BenchmarkElement(ModelCache.getLocal(Loader.currentFile)));
            }
        }));

        return bbce;
    }
}
