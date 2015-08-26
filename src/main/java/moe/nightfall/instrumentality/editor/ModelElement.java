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
import moe.nightfall.instrumentality.PMXInstance;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;

/**
 * Created on 18/08/15.
 */
public class ModelElement extends EditElement {
    PMXInstance workModel;

    public ModelElement() {
        setModel(Loader.currentFile);
        Loader.currentFileListeners.add(new Runnable() {
            @Override
            public void run() {
                setModel(Loader.currentFile);
            }
        });
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
    public void draw(int scrWidth, int scrHeight) {
        super.draw(scrWidth, scrHeight);

        // avoiding perspective "fun" is hard ^.^;
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        GL11.glLoadIdentity();

        GL11.glScaled(2.0d / scrWidth, -2.0d / scrHeight, 1);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, fb);
        GL11.glMultMatrix(fb);
        // Add additional screen-space offsets here
        GL11.glTranslated(getWidth() / 2, getHeight() / 4, 0);
        // --
        GL11.glScaled(scrWidth / 2, -getHeight() / 2, 1);
        GL11.glTranslated(-1, 1, 0);

        GL11.glScaled(3, 3, 1);

        float asp = ((float) scrWidth) / ((float) getHeight());
        GLU.gluPerspective(45, asp, 0.1f, 100);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glTranslated(0, 0, -5);
        float sFactor = 1.0f / workModel.theModel.height;
        GL11.glScaled(sFactor, sFactor, sFactor);
        GL11.glTranslated(0, -(workModel.theModel.height / 2), 0);
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

    private void dumpProject(FloatBuffer fb) {
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, fb);
        System.out.println("x y z w (Transposed : each row is the multiplier from sV)");
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++)
                System.out.print(fb.get() + " ");
            System.out.println();
        }
        fb.rewind();
        System.out.println("-");
    }

    @Override
    public void setSize(int w, int h) {
        super.setSize(w, h);
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }

}