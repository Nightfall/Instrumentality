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
import org.lwjgl.util.vector.Vector3f;

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

    public static Shader shaderBoneTransform;

    public static void main(String[] args) throws Exception {

        shaderBoneTransform = ShaderManager.createProgram("shaders/bone_transform.vert", null);

        FileInputStream fis = new FileInputStream("mdl/mdl.pmx");
        byte[] data = new byte[fis.available()];
        fis.read(data);
        fis.close();
        PMXFile pf = new PMXFile(data);

        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

        PMXModel[] pm = new PMXModel[1];
        PlayerControlAnimation[] pca = new PlayerControlAnimation[pm.length];
        LibraryAnimation[] lib = new LibraryAnimation[pm.length];

        // animation libraries are NOT a per-model thing
        EmoteAnimationLibrary ial_e = new EmoteAnimationLibrary();
        PlayerAnimationLibrary ial_p = new PlayerAnimationLibrary();
        IAnimationLibrary[] animLibs = new IAnimationLibrary[] {ial_e,ial_p};
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
            pm[i] = new PMXModel(pf);

            WalkingAnimation wa = new WalkingAnimation();
            wa.time = i * 0.1f;
            StrengthMultiplyAnimation smaW = new StrengthMultiplyAnimation(wa);

            pca[i] = new PlayerControlAnimation(wa, smaW);
            pca[i].walkingFlag = true;

            lib[i] = new LibraryAnimation();
            lib[i].transitionValue = 1.0f;
            lib[i].setCurrentPose(PlayerAnimationLibrary.createIdlePoseAnimation(),1f,true);

            pm[i].anim = new OverlayAnimation(new IAnimation[]{smaW, pca[i], lib[i]});
        }

        int scrWidth = 1024, scrHeight = 768;
        float rotX = 90, posY = -1, zoom = 7.0f;
        boolean animate = true, eDownLast = false, rDownLast = false;
        Display.setTitle("Instrumentality: PMX Animation Workbench");
        Display.setDisplayMode(new DisplayMode(scrWidth, scrHeight));
        Display.create();
        Mouse.create();

        // Loading shaders
        ShaderManager.loadShaders();

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
        final HashMap<PMXFile.PMXMaterial, Integer> materialTextures = new HashMap<PMXFile.PMXMaterial, Integer>();
        for (PMXFile.PMXMaterial mat : pf.matData) {
            int bTex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, bTex);

            try {
                BufferedImage bi = ImageIO.read(new File("mdl/" + mat.texTex.toLowerCase()));
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
                throw new RuntimeException(e);
            }
            materialTextures.put(mat, bTex);
        }
        Keyboard.create();
        while (!Display.isCloseRequested()) {
            long frameStart = System.currentTimeMillis();
            boolean cobalt = Keyboard.isKeyDown(Keyboard.KEY_C);
            if (cobalt) {
                GL11.glClearColor(0.0f, 0.1f, 0.4f, 1.0f);
            } else {
                GL11.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            }
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

            if (!cobalt)
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            for (int i = 0; i < pm.length; i++) {
                GL11.glPushMatrix();
                GL11.glTranslated(i * 16, 0, 0);
                pm[i].render(new IMaterialBinder() {
                    @Override
                    public void bindMaterial(PMXFile.PMXMaterial texture) {
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, materialTextures.get(texture));
                    }
                }, cobalt);
                GL11.glPopMatrix();
            }
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (cobalt)
                GL11.glColor4d(0.0f, 0.2f, 1.0f, 1.0f);
            for (PMXFile.PMXBone bone : pf.boneData) {
                for (int i = 0; i < pm.length; i++) {
                    Vector3f v3f = pm[i].transformCore(bone, new Vector3f(bone.posX, bone.posY, bone.posZ), false);
                    if (bone.parentBoneIndex != -1) {
                        GL11.glLineWidth(1.0f);
                        GL11.glBegin(GL11.GL_LINES);
                        if (!cobalt)
                            GL11.glColor3d(1, 0, 0);
                        GL11.glVertex3d(v3f.x, v3f.y, v3f.z);
                        if (!cobalt)
                            GL11.glColor3d(0, 1, 0);
                        Vector3f v3f2 = pm[i].transformCore(pf.boneData[bone.parentBoneIndex], new Vector3f(pf.boneData[bone.parentBoneIndex].posX, pf.boneData[bone.parentBoneIndex].posY, pf.boneData[bone.parentBoneIndex].posZ), false);
                        GL11.glVertex3d(v3f2.x, v3f2.y, v3f2.z);
                        GL11.glEnd();
                    }
                    GL11.glPointSize(4);
                    GL11.glBegin(GL11.GL_POINTS);
                    if (!cobalt)
                        GL11.glColor3d(0, 0, 1);
                    GL11.glVertex3d(v3f.x, v3f.y, v3f.z);
                    GL11.glEnd();
                }
            }
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glBegin(GL11.GL_POINTS);

            for (int i = -128; i < 16; i++) {
                GL11.glColor3f(0, 0, 0);
                GL11.glPointSize(4.0f);
                GL11.glVertex3d(0, 0, i / 4.0d);
            }
            GL11.glEnd();

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
                String text=consoleReader.readLine();
                for (IAnimationLibrary ial : animLibs) {
                    IAnimation ia=ial.getPose(text);
                    if (ia!=null) {
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
