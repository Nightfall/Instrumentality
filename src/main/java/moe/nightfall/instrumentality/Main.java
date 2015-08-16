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

import moe.nightfall.instrumentality.animations.*;
import moe.nightfall.instrumentality.animations.libraries.EmoteAnimationLibrary;
import moe.nightfall.instrumentality.animations.libraries.PlayerAnimationLibrary;
import moe.nightfall.instrumentality.shader.Shader;
import moe.nightfall.instrumentality.shader.ShaderManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * FULL LIST OF KEYBOARD CONTROLS:
 * WASD: Camera controls.
 * FV: Zoom control!
 * E: Animation Update Toggle.
 * R: Walking Flag Disable.
 * Up/Down: Turns on/off the walking flag.
 * Left/Right: Time Controls On Unit 00.
 * Shift: Sneak
 * Ctrl: Sprint
 * C(obalt)
 * Q: YOU NEED THE CONSOLE FOR THIS: Lists emotes. Type in the name to apply it.
 * <p/>
 * NOTE: To use the following you need to actually modify the code in places
 * <p/>
 * TYUIO,GHJKL: Controlling some parameters is difficult so this allows live feedback
 * Enter: Dumps live feedback data
 * <p/>
 * These controls exist to be used when working on Emote poses. It simplifies the process quite a bit :)
 * <p/>
 * Before using this code, look in PlayerControlAnimation for some notes
 * <p/>
 * Created on 24/07/15.
 */
public class Main {

    public static String baseDir = "mdl/";

    public static Shader shaderBoneTransform;

    public static PMXFile pf;

    public static PMXModel[] pm;
    public static PlayerControlAnimation[] pca;
    public static LibraryAnimation[] lib;

    public static IAnimationLibrary[] animLibs;
    public static EmoteAnimationLibrary ial_e;
    public static PlayerAnimationLibrary ial_p;

    public static final HashMap<PMXFile.PMXMaterial, Integer> materialTextures = new HashMap<PMXFile.PMXMaterial, Integer>();

    public static void setup() throws Exception {
        loadModel();
        loadShaders();
        loadTextures();
    }

    public static void loadShaders() {
        ShaderManager.loadShaders();
    }

    public static void loadModel() throws Exception {
        // TODO Move
        int groupSize = 12;
        shaderBoneTransform = ShaderManager.createProgram("/assets/instrumentality/shader/bone_transform.vert", null).set("groupSize", groupSize);

        FileInputStream fis = new FileInputStream(baseDir + "mdl.pmx");
        byte[] data = new byte[fis.available()];
        fis.read(data);
        fis.close();
        pf = new PMXFile(data);

        // TODO Proper model loader
        pm = new PMXModel[1];
        pca = new PlayerControlAnimation[pm.length];
        lib = new LibraryAnimation[pm.length];

        // animation libraries are NOT a per-model thing
        ial_e = new EmoteAnimationLibrary();
        ial_p = new PlayerAnimationLibrary();
        animLibs = new IAnimationLibrary[]{ial_e, ial_p};

        for (int i = 0; i < pm.length; i++) {
             /*
              * Animation graph diagram (ASCII)
              * This is not how you need to implement it,
              * but it's how I've implemented it as I've made this testbench
              * If you want the ability to, say, turn off emotes, then put a StrengthMultiply inbetween
              * the Overlay & LibraryAnimation modules.
              *
              * OverlayAnimation-----------------+
              *  |        |                      |
              *  | StrengthMultiplyAnimation LibraryAnimation
              *  | ^      |
              *  | ^>WalkingAnimation
              *  | ^
              * PlayerControlAnimation
              *
              * Note that PCA sends data to other animations for sub-tasks,
              * while doing direct control for others - see arrows for where it sends data to other animations.
              */

            // The minimum for error-free display of the Miku model is 4.
            // The minimum for error-free display of any model is 12.
            pm[i] = new PMXModel(pf, groupSize);

            WalkingAnimation wa = new WalkingAnimation();
            wa.time = i * 0.1f;
            StrengthMultiplyAnimation smaW = new StrengthMultiplyAnimation(wa);

            pca[i] = new PlayerControlAnimation(wa, smaW);
            pca[i].walkingFlag = true;

            lib[i] = new LibraryAnimation();
            lib[i].transitionValue = 1.0f;
            lib[i].setCurrentPose(PlayerAnimationLibrary.createIdlePoseAnimation(), 1f, true);

            pm[i].anim = new OverlayAnimation(new IAnimation[]{smaW, pca[i], lib[i]});
        }
    }

    public static void loadTextures() {
        for (PMXFile.PMXMaterial mat : pf.matData) {
            int bTex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, bTex);

            String str = mat.texTex;
            if (str == null)
                str = "defTex.png";
            try {
                BufferedImage bi = ImageIO.read(new File(baseDir + mat.texTex.toLowerCase()));
                int[] ib = new int[bi.getWidth() * bi.getHeight()];
                bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), ib, 0, bi.getWidth());
                ByteBuffer inb = BufferUtils.createByteBuffer(bi.getWidth() * bi.getHeight() * 4);
                for (int i = 0; i < (bi.getWidth() * bi.getHeight()); i++) {
                    int c = ib[i];
                    inb.put((byte) ((c & 0xFF0000) >> 16));
                    inb.put((byte) ((c & 0xFF00) >> 8));
                    inb.put((byte) (c & 0xFF));
                    inb.put((byte) ((c & 0xFF000000) >> 24));
                }
                inb.rewind();
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, bi.getWidth(), bi.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, inb);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            } catch (Exception e) {
                System.out.println(str);
                throw new RuntimeException(e);
            }
            materialTextures.put(mat, bTex);
        }
    }

    public static void main(String[] args) throws Exception {

        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

        int scrWidth = 800, scrHeight = 600;
        float rotX = 90, posY = -1, zoom = 7.0f;
        boolean animate = true, eDownLast = false, rDownLast = false;
        Display.setTitle("Instrumentality: PMX Animation Workbench");
        Display.setDisplayMode(new DisplayMode(scrWidth, scrHeight));
        Display.create();
        Mouse.create();

        setup();

        GL11.glViewport(0, 0, scrWidth, scrHeight);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        float asp = ((float) scrWidth) / ((float) scrHeight);
        GLU.gluPerspective(45, asp, 0.1f, 100);
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        long frameEndpoint = System.currentTimeMillis();

        Keyboard.create();
        while (!Display.isCloseRequested()) {
            long frameStart = System.currentTimeMillis();
            GL11.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            GL11.glClearDepth(1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glLoadIdentity();

            long ll = System.currentTimeMillis();
            ll &= 8191;
            float testTime = ll / (400.0f);
            GL11.glTranslated(0, posY, -zoom);
            GL11.glRotated(rotX, 0, 1, 0);
            GL11.glTranslated(0, 0, testTime);
            GL11.glPushMatrix();
            GL11.glTranslated(0, 0, -testTime);
            GL11.glScaled(0.2d, 0.2d, 0.2d);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            for (int i = 0; i < pm.length; i++) {
                GL11.glPushMatrix();
                GL11.glTranslated(i * 16, 0, 0);
                pm[i].render(new IMaterialBinder() {
                    @Override
                    public void bindMaterial(PMXFile.PMXMaterial texture) {
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, materialTextures.get(texture));
                    }
                }, shaderBoneTransform);
                GL11.glPopMatrix();
            }
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            GL11.glDisable(GL11.GL_DEPTH_TEST);

            // TODO Bone frame, should move over to PMXModel for debugging
            for (PMXFile.PMXBone bone : pf.boneData) {
                Vector4f v3f = Matrix4f.transform(pm[0].getBoneMatrix(bone), new Vector4f(bone.posX, bone.posY, bone.posZ, 1), null);
                if (bone.parentBoneIndex != -1) {
                    GL11.glLineWidth(1.0f);
                    GL11.glBegin(GL11.GL_LINES);
                    GL11.glColor3d(1, 0, 0);
                    GL11.glVertex3d(v3f.x, v3f.y, v3f.z);
                    GL11.glColor3d(0, 1, 0);
                    Vector4f v3f2 = new Vector4f(pf.boneData[bone.parentBoneIndex].posX, pf.boneData[bone.parentBoneIndex].posY, pf.boneData[bone.parentBoneIndex].posZ, 1);
                    v3f2 = Matrix4f.transform(pm[0].getBoneMatrix(pf.boneData[bone.parentBoneIndex]), v3f2, null);
                    GL11.glVertex3d(v3f2.x, v3f2.y, v3f2.z);
                    GL11.glEnd();
                }
                GL11.glPointSize(4);
                GL11.glBegin(GL11.GL_POINTS);
                GL11.glColor3d(0, 0, 1);
                GL11.glVertex3d(v3f.x, v3f.y, v3f.z);
                GL11.glEnd();
            }
            GL11.glEnable(GL11.GL_DEPTH_TEST);

            GL11.glPopMatrix();

            Display.update();
            Keyboard.poll();
            Mouse.poll();

            long currentTime = System.currentTimeMillis();
            // Frame start is frameEndpoint-20 (note that the delta does include the sleep)
            int delta = (int) (currentTime - (frameEndpoint - 30));
            double deltaTime = delta / 1000.0d;

            if (animate)
                for (int i = 0; i < pm.length; i++)
                    pm[i].update(deltaTime);

            if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
                rotX -= deltaTime * 45;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                rotX += deltaTime * 45;
            }

            boolean eDown = Keyboard.isKeyDown(Keyboard.KEY_E);
            if (eDown)
                if (!eDownLast)
                    animate = !animate;
            eDownLast = eDown;

            boolean rDown = Keyboard.isKeyDown(Keyboard.KEY_R);
            if (rDown)
                if (!rDownLast)
                    pca[0].walkingFlag = !pca[0].walkingFlag;
            rDownLast = rDown;

            if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
                posY += deltaTime;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
                posY -= deltaTime;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_F)) {
                zoom += deltaTime;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_V)) {
                zoom -= deltaTime;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                pca[0].sneakStateTarget = 1;
            } else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                pca[0].sneakStateTarget = -1;
            } else {
                pca[0].sneakStateTarget = 0;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_UP))
                pca[0].walkingFlag = true;
            if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))
                pca[0].walkingFlag = false;

            if (Keyboard.isKeyDown(Keyboard.KEY_T))
                EmoteAnimationLibrary.debugPbt.X0 += deltaTime * 5.0f;
            if (Keyboard.isKeyDown(Keyboard.KEY_Y))
                EmoteAnimationLibrary.debugPbt.Y0 += deltaTime * 5.0f;
            if (Keyboard.isKeyDown(Keyboard.KEY_U))
                EmoteAnimationLibrary.debugPbt.Z0 += deltaTime * 5.0f;
            if (Keyboard.isKeyDown(Keyboard.KEY_I))
                EmoteAnimationLibrary.debugPbt.X1 += deltaTime * 5.0f;
            if (Keyboard.isKeyDown(Keyboard.KEY_O))
                EmoteAnimationLibrary.debugPbt.Y1 += deltaTime * 5.0f;
            if (Keyboard.isKeyDown(Keyboard.KEY_G))
                EmoteAnimationLibrary.debugPbt.X0 -= deltaTime * 5.0f;
            if (Keyboard.isKeyDown(Keyboard.KEY_H))
                EmoteAnimationLibrary.debugPbt.Y0 -= deltaTime * 5.0f;
            if (Keyboard.isKeyDown(Keyboard.KEY_J))
                EmoteAnimationLibrary.debugPbt.Z0 -= deltaTime * 5.0f;
            if (Keyboard.isKeyDown(Keyboard.KEY_K))
                EmoteAnimationLibrary.debugPbt.X1 -= deltaTime * 5.0f;
            if (Keyboard.isKeyDown(Keyboard.KEY_L))
                EmoteAnimationLibrary.debugPbt.Y1 -= deltaTime * 5.0f;
            if (Keyboard.isKeyDown(Keyboard.KEY_RETURN))
                System.out.println(EmoteAnimationLibrary.debugPbt.X0 + "," + EmoteAnimationLibrary.debugPbt.Y0 + "," + EmoteAnimationLibrary.debugPbt.Z0 + "," + EmoteAnimationLibrary.debugPbt.X1 + "," + EmoteAnimationLibrary.debugPbt.Y1);
            if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
                String text = consoleReader.readLine();
                for (IAnimationLibrary ial : animLibs) {
                    IAnimation ia = ial.getPose(text);
                    if (ia != null) {
                        lib[0].setCurrentPose(ia, 8.0f, false);
                        break;
                    }
                }
            }

            long v = frameEndpoint - currentTime;
            if (v > 1)
                Thread.sleep(v);
            Display.setTitle("I-PMXAW: FrameTime " + (currentTime - frameStart) + "ms");
            frameEndpoint = currentTime + 30;

            float eyesX = (Mouse.getX() / (float) scrWidth) - 0.5f;
            float eyesY = (Mouse.getY() / (float) scrHeight) - 0.5f;
            for (PlayerControlAnimation spca : pca) {
                spca.lookLR = eyesX;
                spca.lookUD = eyesY;
            }
        }
        Display.destroy();
    }
}
