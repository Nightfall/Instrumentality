package uk.co.gamemanj.instrumentality;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * The cached IBS transforms. Since the file should never change, this is never cleared.
     */
    private ConcurrentHashMap<PMXFile.PMXBone, Matrix4f> cachedIBSTransforms = new ConcurrentHashMap<PMXFile.PMXBone, Matrix4f>();

    private final IntBuffer[] indexBuffer;

    private final FloatBuffer buffer_v;
    private final FloatBuffer buffer_n;
    private final FloatBuffer buffer_t;

    public PMXTransformThreadPool threadPool;

    public PMXModel(PMXFile pf, PMXTransformThreadPool pttp) {
        theFile = pf;
        indexBuffer = new IntBuffer[pf.matData.length];
        threadPool=pttp;

        buffer_v=BufferUtils.createFloatBuffer(theFile.vertexData.length * 3);
        buffer_n=BufferUtils.createFloatBuffer(theFile.vertexData.length * 3);
        buffer_t=BufferUtils.createFloatBuffer(theFile.vertexData.length * 2);

        int face = 0;

        for(int i = 0; i < theFile.matData.length; i++) {
            PMXFile.PMXMaterial mat = theFile.matData[i];
            indexBuffer[i] = BufferUtils.createIntBuffer(mat.faceCount * 3);
            for(int ind = 0; ind < mat.faceCount;ind++) {
                indexBuffer[i].put(theFile.faceData[face][0]);
                indexBuffer[i].put(theFile.faceData[face][1]);
                indexBuffer[i].put(theFile.faceData[face][2]);
                face++;
            }
        }
        for(int vi = 0; vi < theFile.vertexData.length; vi++) {
            PMXFile.PMXVertex ver = theFile.vertexData[vi];
            buffer_t.put(new float[]{ver.texU, ver.texV});
        }
    }

    /**
     * @param bone The bone to get the IBS of
     * @param translation Disable for normals
     * @return An IBS matrix
     */
    public Matrix4f createIBS(PMXFile.PMXBone bone,boolean translation) {
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

    private Matrix4f getBoneMatrix(PMXFile.PMXBone bone,boolean translation) {
        PoseBoneTransform boneTransform = anim.getBoneTransform(bone.globalName);
        if (boneTransform==null)
            return new Matrix4f();

        Matrix4f t = createIBS(bone,translation);
        Matrix4f i = new Matrix4f();
        Matrix4f.mul(t, i, i);

        Matrix4f bt=new Matrix4f();
        boneTransform.apply(bt);
        Matrix4f.mul(bt, i, i);

        t.invert();
        Matrix4f.mul(t, i, i);
        return i;
    }

    /**
     * Transform a vertex by a bone.
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
                boneTransform.apply(boneMatrix);
                if (!normal) {
                    // first off, bring into the current bone's space
                    Matrix4f intoBoneSpace = cachedIBSTransforms.get(bone);

                    if (intoBoneSpace == null) {
                        intoBoneSpace = createIBS(bone, !normal);
                        cachedIBSTransforms.put(bone, intoBoneSpace);
                    }


                    // Now go into bone space, transform, then leave
                    Vector4f inBoneSpace = Matrix4f.transform(intoBoneSpace, v4f, null);
                    inBoneSpace = Matrix4f.transform(boneMatrix, inBoneSpace, null);
                    Matrix4f leaveBoneSpace = new Matrix4f();
                    Matrix4f.invert(intoBoneSpace, leaveBoneSpace);
                    Matrix4f.transform(leaveBoneSpace, inBoneSpace, v4f);
                } else {
                    v4f = Matrix4f.transform(boneMatrix, v4f, null);
                }
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
     */
    public void render(IMaterialBinder textureBinder) {

        threadPool.transformModel(this, buffer_v, buffer_n);
        buffer_v.rewind();
        buffer_n.rewind();
        buffer_t.rewind();
        for(int i = 0; i < theFile.matData.length; i++) {
            PMXFile.PMXMaterial mat = theFile.matData[i];
            textureBinder.bindMaterial(mat);
            GL11.glColor4d(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            GL11.glVertexPointer(3, 0, buffer_v);
            GL11.glTexCoordPointer(2, 0, buffer_t);
            GL11.glNormalPointer(0, buffer_n);
            indexBuffer[i].rewind();
            GL11.glDrawElements(4, indexBuffer[i]);
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        }
    }

    public void update(double v) {
        if (anim != null)
            anim.update(v);
    }

}
