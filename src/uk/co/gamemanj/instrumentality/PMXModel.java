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
    private HashMap<PMXFile.PMXBone, Matrix4f> cachedIBSTransforms = new HashMap<PMXFile.PMXBone, Matrix4f>();

    private final IntBuffer[] indexBuffer;

    private final FloatBuffer buffer_v;
    private final FloatBuffer buffer_n;
    private final FloatBuffer buffer_t;

    public PMXModel(PMXFile pf) {
        this.theFile = pf;
        this.indexBuffer = new IntBuffer[pf.matData.length];

        buffer_v=BufferUtils.createFloatBuffer(theFile.vertexData.length * 3);
        buffer_n=BufferUtils.createFloatBuffer(theFile.vertexData.length * 3);
        buffer_t=BufferUtils.createFloatBuffer(theFile.vertexData.length * 2);

        int face = 0;

        for(int i = 0; i < theFile.matData.length; i++) {
            PMXFile.PMXMaterial mat = theFile.matData[i];
            this.indexBuffer[i] = BufferUtils.createIntBuffer(mat.faceCount * 3);
            for(int ind = 0; ind < mat.faceCount;ind++) {
                indexBuffer[i].put(theFile.faceData[face][0]);
                indexBuffer[i].put(theFile.faceData[face][1]);
                indexBuffer[i].put(theFile.faceData[face][2]);
                face++;
            }
        }
    }

    /**
     * @param bone The bone to get the IBS of
     * @return An IBS matrix
     */
    public Matrix4f createIBS(PMXFile.PMXBone bone) {
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
        // translate by the inverse position

        intoBoneSpace.rotate(t, new Vector3f(0, 0, 1));
        intoBoneSpace.rotate(p, new Vector3f(1, 0, 0));

        intoBoneSpace.translate(new Vector3f(-(bone.posX), -(bone.posY), -(bone.posZ)));
        return intoBoneSpace;
    }

    private Matrix4f getBoneMatrix(PMXFile.PMXBone bone) {
        PoseBoneTransform boneTransform = anim.getBoneTransform(bone.globalName);
        if (boneTransform==null)
            return new Matrix4f();

        Matrix4f t = createIBS(bone);
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
     * Transform a vertex by a bone. Public because debugging tools.
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
                        intoBoneSpace = createIBS(bone);
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
     * Transform a PMXFile vertex by the PMX file's bone set moved by the model's current animation.
     *
     * @param vert Vertex to transform.
     * @return An array containing the transformed position of the vector.
     */
    public Vector3f[] transformVertex(PMXFile.PMXVertex vert) {
        switch (vert.weightType) {
            case 0:
                return new Vector3f[]{transformCore(theFile.boneData[vert.boneIndices[0]], new Vector3f(vert.posX, vert.posY, vert.posZ), false),
                        transformCore(theFile.boneData[vert.boneIndices[0]], new Vector3f(vert.normalX, vert.normalY, vert.normalZ), true)};
            case 1:
                return new Vector3f[]{ipol2Vec(transformCore(theFile.boneData[vert.boneIndices[0]], new Vector3f(vert.posX, vert.posY, vert.posZ), false), transformCore(theFile.boneData[vert.boneIndices[1]], new Vector3f(vert.posX, vert.posY, vert.posZ), false), vert.boneWeights[0]),
                        ipol2Vec(transformCore(theFile.boneData[vert.boneIndices[0]], new Vector3f(vert.posX, vert.posY, vert.posZ), true), transformCore(theFile.boneData[vert.boneIndices[1]], new Vector3f(vert.posX, vert.posY, vert.posZ), true), vert.boneWeights[0])};
            default:
                // Other weight types won't work in shaders even if you add them here
                throw new RuntimeException("cannot render WT " + vert.weightType);
        }
    }

    /**
     * Only used in the above function.
     * Interpolates with weighting 2 vectors.
     *
     * @param vector3f   Vector A. Note that the data within this argument is modified.
     * @param vector3f1  Vector B.
     * @param boneWeight The weight of the first vector.
     * @return Vector A (reference to vector3f)
     */
    private Vector3f ipol2Vec(Vector3f vector3f, Vector3f vector3f1, float boneWeight) {
        vector3f.x *= boneWeight;
        vector3f.y *= boneWeight;
        vector3f.z *= boneWeight;
        boneWeight = 1.0f - boneWeight;
        vector3f.x += vector3f1.x * boneWeight;
        vector3f.y += vector3f1.y * boneWeight;
        vector3f.z += vector3f1.z * boneWeight;
        return vector3f;
    }

    /**
     * Renders this model, with a given set of textures.
     * Make sure to enable GL_TEXTURE_2D before calling.
     *
     * @param textureBinder Binds a texture.
     */
    public void render(IMaterialBinder textureBinder) {
        int f = 0;

        int i;
        for(i = 0; i < this.theFile.vertexData.length; ++i) {
            PMXFile.PMXVertex mat = theFile.vertexData[i];
            Vector3f[] v3f = transformVertex(mat);
            buffer_v.put(new float[]{v3f[0].x, v3f[0].y, v3f[0].z});
            buffer_n.put(new float[]{v3f[1].x, v3f[1].y, v3f[1].z});
            buffer_t.put(new float[]{mat.texU, mat.texV});
            ++f;
        }

        buffer_v.rewind();
        buffer_n.rewind();
        buffer_t.rewind();

        for(i = 0; i < theFile.matData.length; ++i) {
            PMXFile.PMXMaterial mat = theFile.matData[i];
            textureBinder.bindMaterial(mat);
            GL11.glColor4d(mat.diffR, mat.diffG, mat.diffB, mat.diffA);
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
