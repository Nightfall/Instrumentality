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
        try {
            workModel = new PMXInstance(ModelCache.getLocal(Loader.currentFile));
            workModel.anim = Loader.animLibs[1].getPose("idle");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        Loader.currentFileListeners.add(new Runnable() {
            @Override
            public void run() {
                workModel.cleanupGL();
                try {
                    workModel = new PMXInstance(ModelCache.getLocal(Loader.currentFile));
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        });
        examplePanel.setSize(160, 100);
        examplePanel.posX = 8;
        examplePanel.posY = 8;
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
