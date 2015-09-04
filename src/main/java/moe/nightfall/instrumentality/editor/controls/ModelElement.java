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
package moe.nightfall.instrumentality.editor.controls;

import moe.nightfall.instrumentality.Loader;
import moe.nightfall.instrumentality.ModelCache;
import moe.nightfall.instrumentality.PMXInstance;
import moe.nightfall.instrumentality.PMXModel;
import moe.nightfall.instrumentality.animations.IAnimation;
import moe.nightfall.instrumentality.animations.OverlayAnimation;
import moe.nightfall.instrumentality.animations.WalkingAnimation;
import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.UIUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;

/**
 * Created on 18/08/15.
 */
public class ModelElement extends EditElement {
    private PMXInstance workModel;
    private String workModelName;
    public boolean isButton, isHover;
    public double rotYaw,rotPitch;
    
    public ModelElement(boolean ib) {
        isButton=ib;
        if (!isButton) {
            setModel(Loader.currentFile);
            Loader.currentFileListeners.add(new Runnable() {
                @Override
                public void run() {
                    setModel(Loader.currentFile);
                }
            });
        }
    }

    public void setModel(String modelName) {
        if (workModel != null) {
            workModel.cleanupGL();
            workModel = null;
        }
        workModelName = modelName;
        if (modelName == null)
            return;
        PMXModel b = ModelCache.getLocal(modelName);
        if (b == null)
            return;
        workModel = makeTestInstance(b);
    }

    public static PMXInstance makeTestInstance(PMXModel pm) {
        PMXInstance pi = new PMXInstance(pm);
        WalkingAnimation wa=new WalkingAnimation();
        wa.speed=1.0f;
        pi.anim = new OverlayAnimation(new IAnimation[]{Loader.animLibs[1].getPose("idle"), wa});
        return pi;
    }

    @Override
    public void draw(int scrWidth, int scrHeight) {
        if (isButton) {
            if (workModelName==null) {
                colourStrength=0.25f;
                colourStrength *= isHover ? 1.4f : 1f;
            } else {
                colourStrength=0.5f;
                if (workModelName.equalsIgnoreCase(Loader.currentFile))
                    colourStrength=0.75f;
                colourStrength *= isHover ? 1.2f : 1f;
            }
        }
        super.draw(scrWidth, scrHeight);
        if (isButton) {
            String text = "<null>";
            if (workModelName != null)
                text = workModelName;
            GL11.glPushMatrix();
            GL11.glTranslated(borderWidth, borderWidth, 0);
            GL11.glScaled(2, 2, 1);
            UIUtils.drawText(text, 2);
            GL11.glPopMatrix();
        }

        if (workModel==null)
            return;

        // avoiding perspective "fun" is hard ^.^;
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, fb);

        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glTranslated(0, 1, 0);
        
        GL11.glScaled(2.0d / scrWidth, -2.0d / scrHeight, 1);
        GL11.glMultMatrix(fb);
        // Add additional screen-space offsets here
        GL11.glTranslated(getWidth() / 2.0, getHeight() / 3.0, 0);
        // --
        GL11.glScaled(scrWidth / 2.0, -scrHeight / 2.0, 1);
        
        GL11.glTranslated(-1, 0, 0);

        GL11.glScaled(3, 3, 1);

        GL11.glScaled(1, getHeight()/(float)scrHeight, 1);

        GL11.glTranslated(0, -0.1, 0);

        float asp = ((float) scrWidth) / ((float) getHeight());
        GLU.gluPerspective(45, asp, 0.1f, 100);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glTranslated(0, 0, -5);
        float sFactor = 1.0f / workModel.theModel.height;
        GL11.glScaled(sFactor, sFactor, sFactor);
        GL11.glTranslated(0, -(workModel.theModel.height / 2), 0);
        GL11.glRotated(rotPitch, 1, 0, 0);
        GL11.glRotated(rotYaw, 0, 1, 0);

        GL11.glDisable(GL11.GL_CULL_FACE);
        if (workModel != null)
            workModel.render(Loader.shaderBoneTransform, 1, 1, 1, workModel.theModel.height+1.0f);
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
    
    private int dragX=0,dragY=0;
    private boolean ignoreFirstDrag=false;
    
    @Override
    public void mouseMove(int x, int y, boolean[] buttons) {
        if (buttons[0]) {
            if (!ignoreFirstDrag) {
                rotYaw+=x-dragX;
                rotPitch+=y-dragY;
            }
            ignoreFirstDrag=false;
            dragX=x;
            dragY=y;
        }
    }
    
    @Override
    public void mouseStateChange(int x, int y, boolean isDown, boolean isRight) {
        super.mouseStateChange(x, y, isDown, isRight);
        if (isDown&&(!isRight)) {
            ignoreFirstDrag=true;
            if (isButton) {
                Loader.setCurrentFile(workModelName);
            }
        }
    }
    
    @Override
    public void mouseEnterLeave(boolean isInside) {
        super.mouseEnterLeave(isInside);
        ignoreFirstDrag=true;
        isHover = isInside;
    }
    
    public void update(double dTime) {
        super.update(dTime);
        if (workModel != null)
            workModel.update(dTime);
    }
}
