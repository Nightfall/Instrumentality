package uk.co.gamemanj.instrumentality;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created on 26/07/15.
 */
public class PMXTransformThread extends Thread {
    public PMXTransformThreadPool pttp;
    public int stride, ofs;
    // Keep-alive. If this times out (30 seconds),
    public long lastUpdate = 0;
    // Or this is switched on...
    public boolean killSwitch = false;
    // Or this thread dies...(set to this thread for an "ignore")
    public Thread monitorThread;
    // Then the thread exits.

    // These exist because they're easier to pass around as instance vars
    private Matrix4f[] matrix4fTs, matrix4fNTs;


    /**
     * Transform a PMXFile vertex by the PMX file's bone set moved by the model's current animation.
     *
     * @param vert Vertex to transform.
     * @return An array containing the transformed position of the vector.
     */
    private Vector3f[] transformVertex(PMXFile.PMXVertex vert) {
        switch (vert.weightType) {
            case 0:
                return new Vector3f[]{t43(Matrix4f.transform(matrix4fTs[vert.boneIndices[0]], new Vector4f(vert.posX, vert.posY, vert.posZ, 1.0f), null)),
                        t43(Matrix4f.transform(matrix4fNTs[vert.boneIndices[0]], new Vector4f(vert.posX, vert.posY, vert.posZ, 1.0f), null))};
            case 1:
                return weightedTransform(vert, new int[]{vert.boneIndices[0], vert.boneIndices[1]}, new float[]{vert.boneWeights[0], 1.0f - vert.boneWeights[0]});
            case 2:
                return weightedTransform(vert, vert.boneIndices, vert.boneWeights);
            default:
                throw new RuntimeException("cannot render WT " + vert.weightType);
        }
    }

    private Vector3f[] weightedTransform(PMXFile.PMXVertex vert, int[] bones, float[] floats) {
        Vector3f[] bT = new Vector3f[bones.length];
        Vector3f[] bNT = new Vector3f[bones.length];
        for (int i = 0; i < bones.length; i++) {
            bT[i] = t43(Matrix4f.transform(matrix4fTs[bones[i]], new Vector4f(vert.posX, vert.posY, vert.posZ, 1.0f), null));
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
        return new Vector3f(transform.x, transform.y, transform.z);
    }

    private void processModel(PMXTransformThreadPool.PMXTransformingModel model) {
        matrix4fTs = new Matrix4f[model.pmxModel.theFile.boneData.length];
        matrix4fNTs = new Matrix4f[model.pmxModel.theFile.boneData.length];
        for (int i = 0; i < model.pmxModel.theFile.boneData.length; i++) {
            matrix4fTs[i] = model.pmxModel.getBoneMatrix(model.pmxModel.theFile.boneData[i], true);
            matrix4fNTs[i] = model.pmxModel.getBoneMatrix(model.pmxModel.theFile.boneData[i], false);
        }
        for (int i = 0; i < model.pmxModel.theFile.vertexData.length; i++) {
            PMXFile.PMXVertex mat = model.pmxModel.theFile.vertexData[i];
            Vector3f[] v3f = transformVertex(mat);
            model.vertexBuffer[(i * 3) + 0] = v3f[0].x;
            model.vertexBuffer[(i * 3) + 1] = v3f[0].y;
            model.vertexBuffer[(i * 3) + 2] = v3f[0].z;
            model.normalBuffer[(i * 3) + 0] = v3f[1].x;
            model.normalBuffer[(i * 3) + 1] = v3f[1].y;
            model.normalBuffer[(i * 3) + 2] = v3f[1].z;
        }
    }

    @Override
    public void run() {
        long frameEndpoint = System.currentTimeMillis();
        while (((lastUpdate + 30000) > System.currentTimeMillis()) && (!killSwitch) && monitorThread.isAlive()) {
            PMXTransformThreadPool.PMXTransformingModel[] a = pttp.modelSet.values().toArray(new PMXTransformThreadPool.PMXTransformingModel[0]);
            for (int i = ofs; i < a.length; i += stride)
                processModel(a[i]);
            long currentTime = System.currentTimeMillis();
            long v = frameEndpoint - currentTime;
            try {
                if (v > 1)
                    Thread.sleep(v);
            } catch (InterruptedException ie) {

            }
            frameEndpoint = currentTime + 50;
        }
    }
}
