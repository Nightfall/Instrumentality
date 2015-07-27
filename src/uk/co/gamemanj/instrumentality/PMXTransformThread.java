package uk.co.gamemanj.instrumentality;

import org.lwjgl.Sys;
import org.lwjgl.util.vector.Matrix;
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
    private Matrix4f[] matrix4fTs,matrix4fNTs;
    /**
     * Used to check that we haven't been silently discarded
     */
    public long lastUpdateTime;

    /**
     * Transform a PMXFile vertex by the PMX file's bone set moved by the model's current animation.
     *
     * @param vert Vertex to transform.
     * @return An array containing the transformed position of the vector.
     */
    private Vector3f[] transformVertex(PMXFile.PMXVertex vert) {
        switch (vert.weightType) {
            case 0:
                return new Vector3f[]{t43(Matrix4f.transform(matrix4fTs[vert.boneIndices[0]], new Vector4f(vert.posX,vert.posY,vert.posZ,1.0f),null)),
                        t43(Matrix4f.transform(matrix4fNTs[vert.boneIndices[0]], new Vector4f(vert.posX, vert.posY, vert.posZ, 1.0f), null))};
            case 1:
                return weightedTransform(vert, new int[]{vert.boneIndices[0], vert.boneIndices[1]}, new float[]{vert.boneWeights[0], 1.0f - vert.boneWeights[0]});
            case 2:
                return weightedTransform(vert, vert.boneIndices, vert.boneWeights);
            default:
                throw new RuntimeException("cannot render WT " + vert.weightType);
        }
    }

    private Vector3f[] weightedTransform(PMXFile.PMXVertex vert, int[] bones,float[] floats) {
        Vector3f[] bT = new Vector3f[bones.length];
        Vector3f[] bNT = new Vector3f[bones.length];
        for (int i = 0; i < bones.length; i++) {
            bT[i] = t43(Matrix4f.transform(matrix4fTs[bones[i]], new Vector4f(vert.posX,vert.posY,vert.posZ,1.0f),null));
            bT[i].scale(floats[i]);
            bNT[i] = t43(Matrix4f.transform(matrix4fNTs[bones[i]], new Vector4f(vert.posX, vert.posY, vert.posZ, 1.0f), null));
            bNT[i].scale(floats[i]);
        }
        Vector3f[] results = new Vector3f[2];
        results[0] = new Vector3f(0, 0, 0);
        results[1] = new Vector3f(0, 0, 0);
        for (int i = 0; i < bones.length; i++) {
            Vector3f.add(results[0], bT[i], results[0]);
            Vector3f.add(results[1], bNT[i], results[1]);
        }
        return results;
    }

    private Vector3f t43(Vector4f transform) {
        return new Vector3f(transform.x,transform.y,transform.z);
    }

    @Override
    public void run() {
        matrix4fTs=new Matrix4f[model.theFile.boneData.length];
        matrix4fNTs=new Matrix4f[model.theFile.boneData.length];
        long frameEndpoint=System.currentTimeMillis();
        while (System.currentTimeMillis()<(lastUpdateTime+2000)) {
            for (int i=0;i<model.theFile.boneData.length;i++) {
                matrix4fTs[i] = model.getBoneMatrix(model.theFile.boneData[i], true);
                matrix4fNTs[i] = model.getBoneMatrix(model.theFile.boneData[i], false);
            }
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
            long currentTime=System.currentTimeMillis();
            long v=frameEndpoint-currentTime;
            try {
                if (v > 1)
                    Thread.sleep(v);
            } catch (InterruptedException ie) {

            }
            // The animations do *NOT NEED* more than 30 FPS
            frameEndpoint=currentTime+32;
        }
    }
}
