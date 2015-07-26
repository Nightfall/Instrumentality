package uk.co.gamemanj.instrumentality;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;

/**
 * Created on 26/07/15.
 */
public class PMXTransformThread extends Thread {
    public PMXModel model;
    public float[] vertexArray;
    public float[] normalArray;
    public int stride, ofs;

    /**
     * Transform a PMXFile vertex by the PMX file's bone set moved by the model's current animation.
     *
     * @param vert Vertex to transform.
     * @return An array containing the transformed position of the vector.
     */
    private Vector3f[] transformVertex(PMXFile.PMXVertex vert) {
        switch (vert.weightType) {
            case 0:
                return new Vector3f[]{model.transformCore(model.theFile.boneData[vert.boneIndices[0]], new Vector3f(vert.posX, vert.posY, vert.posZ), false),
                        model.transformCore(model.theFile.boneData[vert.boneIndices[0]], new Vector3f(vert.posX, vert.posY, vert.posZ), true)};
            case 1:
                return weightedTransform(vert, new int[]{vert.boneIndices[0], vert.boneIndices[1]}, new float[]{vert.boneWeights[0], 1.0f - vert.boneWeights[0]});
            case 2:
                return weightedTransform(vert, vert.boneIndices, vert.boneWeights);
            default:
                throw new RuntimeException("cannot render WT " + vert.weightType);
        }
    }

    private Vector3f[] weightedTransform(PMXFile.PMXVertex vert, int[] matrix4fs, float[] floats) {
        Vector3f[] bT = new Vector3f[matrix4fs.length];
        Vector3f[] bNT = new Vector3f[matrix4fs.length];
        for (int i = 0; i < matrix4fs.length; i++) {
            bT[i] = model.transformCore(model.theFile.boneData[matrix4fs[i]], new Vector3f(vert.posX, vert.posY, vert.posZ), false);
            bT[i].scale(floats[i]);
            bNT[i] = model.transformCore(model.theFile.boneData[matrix4fs[i]], new Vector3f(vert.posX, vert.posY, vert.posZ), true);
            bNT[i].scale(floats[i]);
        }
        Vector3f[] results = new Vector3f[2];
        results[0] = new Vector3f(0, 0, 0);
        results[1] = new Vector3f(0, 0, 0);
        for (int i = 0; i < matrix4fs.length; i++) {
            Vector3f.add(results[0], bT[i], results[0]);
            Vector3f.add(results[1], bNT[i], results[1]);
        }
        return results;
    }

    @Override
    public void run() {
        for (int i = ofs; i < model.theFile.vertexData.length; i += stride) {
            PMXFile.PMXVertex mat = model.theFile.vertexData[i];
            Vector3f[] v3f = transformVertex(mat);
            vertexArray[(i * 3) + 0] = v3f[0].x;
            vertexArray[(i * 3) + 1] = v3f[0].y;
            vertexArray[(i * 3) + 2] = v3f[0].z;
            normalArray[(i * 3) + 0] = v3f[1].x;
            normalArray[(i * 3) + 1] = v3f[1].y;
            normalArray[(i * 3) + 2] = v3f[1].z;
        }
    }
}
