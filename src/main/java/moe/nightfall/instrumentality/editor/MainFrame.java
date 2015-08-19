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
import moe.nightfall.instrumentality.Main;
import moe.nightfall.instrumentality.ModelCache;
import moe.nightfall.instrumentality.PMXInstance;
import moe.nightfall.instrumentality.animations.LibraryAnimation;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.io.IOException;

/**
 * Created on 18/08/15.
 */
public class MainFrame extends EditElement {
    EditElement examplePanel = new EditElement();
    PMXInstance workModel;

    public MainFrame() {
        setModel(Loader.currentFile);
        Loader.currentFileListeners.add(new Runnable() {
            @Override
            public void run() {
                setModel(Loader.currentFile);
            }
        });
        examplePanel.setSize(160, 100);
        examplePanel.posX = 8;
        examplePanel.posY = 8;
    }

    public void setModel(String modelName) {
        if (workModel != null) {
            workModel.cleanupGL();
            workModel = null;
        }
        if (modelName == null)
            return;
        workModel = new PMXInstance(ModelCache.getLocal(modelName));
        workModel.anim = Loader.animLibs[1].getPose("idle");
    }

    @Override
    public void draw() {
        drawSkinnedRect(posX, posY, getWidth(), getHeight(), 1.0f);
        examplePanel.draw();

        // 3D Drawing
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        float asp = ((float) getWidth()) / ((float) getHeight());
        GLU.gluPerspective(45, asp, 0.1f, 100);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslated(0, -1, -5f);
        GL11.glScaled(0.1f, 0.1f, 0.1f);
        GL11.glRotated(Math.toDegrees((System.currentTimeMillis() % 6282) / 1000.0d), 0, 1, 0);
        GL11.glDisable(GL11.GL_CULL_FACE);
        if (workModel != null)
            workModel.render(Loader.shaderBoneTransform);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    @Override
    public void setSize(int w, int h) {
        super.setSize(w, h);
    }
}
