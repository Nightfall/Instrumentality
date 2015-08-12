package moe.nightfall.instrumentality;

import moe.nightfall.instrumentality.animations.IAnimation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Allows animating a model file, and rendering using LWJGL.
 * Note that changing the bone data (specifically the bone data) while a PMXModel is attached to it is not recommended without replacing the object.
 * The assumption is that PMXFile will never be used for editing.
 *
 * @author gamemanj
 *         Created on 24/07/15.
 */
public class PMXModel {
    public final PMXFile theFile;
    /**
     * Animation. Can be changed at any time.
     */
    public IAnimation anim;

    private final IntBuffer[] indexBuffer;
    private final IntBuffer[] cobaltIndexBuffer;

    private final FloatBuffer buffer_v;
    private final FloatBuffer buffer_n;
    private final FloatBuffer buffer_t;

    public PMXModel(PMXFile pf) {
        theFile = pf;
        indexBuffer = new IntBuffer[pf.matData.length];
        cobaltIndexBuffer = new IntBuffer[pf.matData.length];

        buffer_v = BufferUtils.createFloatBuffer(theFile.vertexData.length * 3);
        buffer_n = BufferUtils.createFloatBuffer(theFile.vertexData.length * 3);
        buffer_t = BufferUtils.createFloatBuffer(theFile.vertexData.length * 2);

        int face = 0;

        for (int i = 0; i < theFile.matData.length; i++) {
            PMXFile.PMXMaterial mat = theFile.matData[i];
            indexBuffer[i] = BufferUtils.createIntBuffer(mat.faceCount * 3);
            cobaltIndexBuffer[i] = BufferUtils.createIntBuffer(mat.faceCount * 6);
            for (int ind = 0; ind < mat.faceCount; ind++) {
                cobaltIndexBuffer[i].put(theFile.faceData[face][0]);
                cobaltIndexBuffer[i].put(theFile.faceData[face][1]);

                cobaltIndexBuffer[i].put(theFile.faceData[face][1]);
                cobaltIndexBuffer[i].put(theFile.faceData[face][2]);

                cobaltIndexBuffer[i].put(theFile.faceData[face][2]);
                cobaltIndexBuffer[i].put(theFile.faceData[face][0]);

                indexBuffer[i].put(theFile.faceData[face][0]);
                indexBuffer[i].put(theFile.faceData[face][1]);
                indexBuffer[i].put(theFile.faceData[face][2]);
                face++;
            }
        }
        for (int vi = 0; vi < theFile.vertexData.length; vi++) {
            PMXFile.PMXVertex ver = theFile.vertexData[vi];
            buffer_v.put(new float[]{ver.posX, ver.posY, ver.posZ});
            buffer_n.put(new float[]{ver.normalX, ver.normalY, ver.normalZ});
            buffer_t.put(new float[]{ver.texU, ver.texV});
        }
    }

    /**
     * This used to be created in transformCore, then cached forever
     * now that's no longer needed, as the entire transform matrix is cached for the frame -
     * caching this is a waste of time now.
     *
     * @param bone        The bone to get the IBS of
     * @param translation Disable for normals
     * @return An IBS matrix
     */
    public Matrix4f createIBS(PMXFile.PMXBone bone, boolean translation) {
        float dX = bone.connectionPosOfsX;
        float dY = bone.connectionPosOfsY;
        float dZ = bone.connectionPosOfsZ;
        if (bone.flagConnection) {
            if (bone.connectionIndex == -1) {
                dX = 0;
                dY = 1;
                dZ = 0;
            } else {
                dX = theFile.boneData[bone.connectionIndex].posX - bone.posX;
                dY = theFile.boneData[bone.connectionIndex].posY - bone.posY;
                dZ = theFile.boneData[bone.connectionIndex].posZ - bone.posZ;
            }
        }
        double magnitude = Math.sqrt((dX * dX) + (dY * dY) + (dZ * dZ));
        float t = (float) Math.atan(dY / dX);
        float p = (float) Math.acos(dZ / magnitude);
        Matrix4f intoBoneSpace = new Matrix4f();

        intoBoneSpace.rotate(t, new Vector3f(0, 0, 1));
        intoBoneSpace.rotate(p, new Vector3f(1, 0, 0));

        // translate by the inverse position
        if (translation)
            intoBoneSpace.translate(new Vector3f(-(bone.posX), -(bone.posY), -(bone.posZ)));
        return intoBoneSpace;
    }

    public Matrix4f getBoneMatrix(PMXFile.PMXBone bone, boolean translation) {
        PoseBoneTransform boneTransform = anim.getBoneTransform(compatibilityCheck(bone.globalName));
        Matrix4f i = new Matrix4f();
        if (boneTransform != null) {
            Matrix4f t = createIBS(bone, translation);
            Matrix4f.mul(t, i, i);

            Matrix4f bt = new Matrix4f();
            boneTransform.apply(bt, translation);
            Matrix4f.mul(bt, i, i);

            t.invert();
            Matrix4f.mul(t, i, i);
        }
        if (bone.parentBoneIndex != -1)
            Matrix4f.mul(getBoneMatrix(theFile.boneData[bone.parentBoneIndex], translation), i, i);
        return i;
    }

    /**
     * Some models use different names for what are essentially the same bones.
     * Put checks here to find these.
     * <p/>
     * Note that if animations don't work correctly with the new name of a bone,
     * it is better to put the compatibility in the affected animations
     * (so the values can be adjusted to compensate)
     *
     * @param globalName The original globalName of the bone.
     * @return The translated bone name, or the original if it is not in the compatibility table.
     */
    private String compatibilityCheck(String globalName) {

        // Kagamine Rin Legs

        if (globalName.equalsIgnoreCase("L_leg"))
            return "leg_L";
        if (globalName.equalsIgnoreCase("L_knee"))
            return "knee_L";
        if (globalName.equalsIgnoreCase("L_foot"))
            return "ankle_L";

        if (globalName.equalsIgnoreCase("R_leg"))
            return "leg_R";
        if (globalName.equalsIgnoreCase("R_knee"))
            return "knee_R";
        if (globalName.equalsIgnoreCase("R_foot"))
            return "ankle_R";

        return globalName;
    }

    /**
     * Transform a vertex by a bone.
     * By now, this has become "only for comparison to getBoneMatrix for testing",
     * as getBoneMatrix does much the same job but does it by returning one matrix,
     * which allows said matrix to be cached across the entire transform.
     * If this ever doesn't match getBoneMatrix, either this is outdated due to something new,
     * or getBoneMatrix has a bug in it.
     */
    public Vector3f transformCore(PMXFile.PMXBone bone, Vector3f vIn, boolean normal) {
        if (anim == null)
            return vIn; // Completely NOP if we're not animating.
        Vector4f v4f = new Vector4f(vIn.x, vIn.y, vIn.z, 1);
        while (bone != null) {
            PoseBoneTransform boneTransform = anim.getBoneTransform(bone.globalName);
            // If we're not transforming this bone, don't waste time
            if (boneTransform != null) {
                Matrix4f boneMatrix = new Matrix4f();
                boneTransform.apply(boneMatrix, !normal);
                // first off, bring into the current bone's space
                Matrix4f intoBoneSpace = createIBS(bone, !normal);

                // Now go into bone space, transform, then leave
                Vector4f inBoneSpace = Matrix4f.transform(intoBoneSpace, v4f, null);
                inBoneSpace = Matrix4f.transform(boneMatrix, inBoneSpace, null);
                Matrix4f leaveBoneSpace = new Matrix4f();
                Matrix4f.invert(intoBoneSpace, leaveBoneSpace);
                Matrix4f.transform(leaveBoneSpace, inBoneSpace, v4f);
            }

            if (bone.parentBoneIndex == -1) {
                bone = null;
            } else {
                bone = theFile.boneData[bone.parentBoneIndex];
            }
        }
        return new Vector3f(v4f.x, v4f.y, v4f.z);
    }

    /**
     * Renders this model, with a given set of textures.
     * Make sure to enable GL_TEXTURE_2D before calling.
     *
     * @param textureBinder Binds a texture.
     * @param cobalt        undocumented feature
     */
    public void render(IMaterialBinder textureBinder, boolean cobalt) {

//        threadPool.transformModel(this, buffer_v, buffer_n);
        buffer_v.rewind();
        buffer_n.rewind();
        buffer_t.rewind();
        for (int pass = 0; pass < (cobalt ? 5 : 1); pass++) {
            for (int i = 0; i < theFile.matData.length; i++) {
                cobaltIndexBuffer[i].rewind();
                indexBuffer[i].rewind();
                PMXFile.PMXMaterial mat = theFile.matData[i];
                textureBinder.bindMaterial(mat);
                if (cobalt) {
                    float mul = new float[]{
                            0.1f,
                            0.2f,
                            0.55f,
                            0.75f,
                            1.0f
                    }[pass];
                    GL11.glLineWidth(5 - pass);
                    //GL11.glClearColor(0.0f, 0.1f, 0.4f, 1.0f);
                    GL11.glColor4d(0.0f, 0.1f + (0.1f * mul), 0.4f + (0.6f * mul), 1.0f);
                } else {
                    GL11.glColor4d(1.0f, 1.0f, 1.0f, 1.0f);
                }
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                GL11.glVertexPointer(3, 0, buffer_v);
                GL11.glTexCoordPointer(2, 0, buffer_t);
                GL11.glNormalPointer(0, buffer_n);
                if (cobalt) {
                    GL11.glDrawElements(GL11.GL_LINES, cobaltIndexBuffer[i]);
                } else {
                    GL11.glDrawElements(GL11.GL_TRIANGLES, indexBuffer[i]);
                }
                GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            }
        }
    }

    public void update(double v) {
        if (anim != null)
            anim.update(v);
    }

}
