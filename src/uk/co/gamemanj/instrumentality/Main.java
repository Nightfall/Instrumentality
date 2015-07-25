package uk.co.gamemanj.instrumentality;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Color;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

/**
 * Created on 24/07/15.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        FileInputStream fis = new FileInputStream("mdl/mdl.pmx");
        byte[] data = new byte[fis.available()];
        fis.read(data);
        fis.close();
        PMXFile pf = new PMXFile(data);
        PMXModel pm = new PMXModel(pf);
        WalkingAnimation wa=new WalkingAnimation();
        FadeInAnimation fia=new FadeInAnimation(wa);
        pm.anim=fia;
        int scrWidth=800,scrHeight=600;
        float rotX=90, posY=-1;
        boolean animate=false,eDownLast=false;
        Display.setTitle("Miku Test");
        Display.setDisplayMode(new DisplayMode(scrWidth, scrHeight));
        Display.create();
        GL11.glViewport(0, 0, scrWidth, scrHeight);

        GL11.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        GL11.glClearDepth(1.0f);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        float asp = ((float) scrWidth) / ((float) scrHeight);
        GLU.gluPerspective(45, asp, 0.1f, 100);
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        long frameEndpoint=System.currentTimeMillis();
        final HashMap<PMXFile.PMXMaterial, Integer> materialTextures=new HashMap<>();
        for (PMXFile.PMXMaterial mat : pf.matData) {
            int bTex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, bTex);

            try {
                BufferedImage bi = ImageIO.read(new File("mdl/" + mat.texTex.toLowerCase()));
                int[] ib=new int[bi.getWidth()*bi.getHeight()];
                bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), ib, 0, bi.getWidth());
                ByteBuffer inb=BufferUtils.createByteBuffer(bi.getWidth() * bi.getHeight() * 4);
                for (int i=0;i<(bi.getWidth()*bi.getHeight());i++) {
                    int c=ib[i];
                    inb.put((byte)((c&0xFF0000)>>16));
                    inb.put((byte)((c&0xFF00)>>8));
                    inb.put((byte)(c&0xFF));
                    inb.put((byte)((c&0xFF000000)>>24));
                }
                inb.rewind();
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, bi.getWidth(), bi.getHeight(), 0, GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,inb);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            materialTextures.put(mat,bTex);
        }
        Keyboard.create();
        while (!Display.isCloseRequested()) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glLoadIdentity();

            long ll=System.currentTimeMillis();
            ll&=8191;
            float testTime=ll/(400.0f);
            GL11.glTranslated(0, posY, -7.0d);
            GL11.glRotated(rotX, 0, 1, 0);
            GL11.glTranslated(0, 0, testTime);
            GL11.glPushMatrix();
            GL11.glTranslated(0, 0, -testTime);
            GL11.glScaled(0.2d, 0.2d, 0.2d);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            pm.render(new IMaterialBinder() {
                @Override
                public void bindMaterial(PMXFile.PMXMaterial texture) {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, materialTextures.get(texture));
                }
            });
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            for (PMXFile.PMXBone bone : pf.boneData) {
                GL11.glPointSize(2);
                Vector3f v3f = pm.transformCore(bone, new Vector3f(bone.posX, bone.posY, bone.posZ), false);
                if (bone.parentBoneIndex != -1) {
                    GL11.glBegin(GL11.GL_LINES);
                    GL11.glColor3d(1, 0, 0);
                    GL11.glVertex3d(v3f.x, v3f.y, v3f.z);
                    GL11.glColor3d(0, 1, 0);
                    Vector3f v3f2 = pm.transformCore(pf.boneData[bone.parentBoneIndex], new Vector3f(pf.boneData[bone.parentBoneIndex].posX, pf.boneData[bone.parentBoneIndex].posY, pf.boneData[bone.parentBoneIndex].posZ),false);
                    GL11.glVertex3d(v3f2.x, v3f2.y, v3f2.z);
                    GL11.glEnd();
                }
                GL11.glPointSize(4);
                GL11.glBegin(GL11.GL_POINTS);
                GL11.glColor3d(0, 0, 1);
                GL11.glVertex3d(v3f.x, v3f.y, v3f.z);
                GL11.glEnd();
            }
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glBegin(GL11.GL_POINTS);

            for (int i=-128;i<16;i++) {
                GL11.glColor3f(0,0,0);
                GL11.glPointSize(4.0f);
                GL11.glVertex3d(0,0,i/4.0d);
            }
            GL11.glEnd();
            Display.update();
            long currentTime=System.currentTimeMillis();
            // Frame start is frameEndpoint-20 (note that the delta does include the sleep)
            int delta=(int)(currentTime-(frameEndpoint-30));
            double deltaTime=delta/1000.0d;

            if (animate)
                pm.update(deltaTime);

            Keyboard.poll();
            if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
                rotX-=deltaTime*45;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                rotX+=deltaTime*45;
            }
            boolean eDown=Keyboard.isKeyDown(Keyboard.KEY_E);
            if (eDown)
                if (!eDownLast)
                    animate=!animate;
            eDownLast=eDown;
            if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
                posY+=deltaTime;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
                posY-=deltaTime;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                fia.mulAmount -= deltaTime;
                if (fia.mulAmount<0)
                    fia.mulAmount=0;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                fia.mulAmount += deltaTime;
                if (fia.mulAmount>1)
                    fia.mulAmount=1;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
                wa.time -= deltaTime;
                while (wa.time<0)
                    wa.time+=1;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
                wa.time+=deltaTime;
                while (wa.time>1)
                    wa.time-=1;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_RETURN)) {
                System.out.println(wa.time);
                wa.time=0;
            }
            long v=frameEndpoint-currentTime;
            if (v>1)
                Thread.sleep(v);
            frameEndpoint=currentTime+30;
        }
        Display.destroy();
    }
}
