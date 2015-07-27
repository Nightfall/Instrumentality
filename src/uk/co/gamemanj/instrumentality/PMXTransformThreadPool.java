package uk.co.gamemanj.instrumentality;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created on 26/07/15.
 */
public class PMXTransformThreadPool {
    private ConcurrentHashMap<PMXModel, PMXTransformingModel> modelSet = new ConcurrentHashMap<PMXModel, PMXTransformingModel>();
    private int coresPerModel;

    public PMXTransformThreadPool(int cpm) {
        coresPerModel = cpm;
    }

    public void addModel(PMXModel pm) {
        PMXTransformingModel ptm = new PMXTransformingModel();
        ptm.pmxModel = pm;
        ptm.vertexBuffer = new float[pm.theFile.vertexData.length * 3];
        ptm.normalBuffer = new float[pm.theFile.vertexData.length * 3];
        ptm.thePool = new PMXTransformThread[coresPerModel];
        for (int i = 0; i < coresPerModel; i++) {
            PMXTransformThread ptt = ptm.thePool[i] = new PMXTransformThread();
            ptt.ofs = i;
            ptt.stride = coresPerModel;
            ptt.model = pm;
            ptt.vertexArray = ptm.vertexBuffer;
            ptt.normalArray = ptm.normalBuffer;
            ptt.lastUpdateTime = System.currentTimeMillis();
            ptt.start();
        }
        modelSet.put(pm, ptm);
    }

    /**
     * Transform the model by the current set of transforms
     *
     * @param model
     * @param vertexArray
     * @param normalArray
     */
    public void transformModel(PMXModel model, FloatBuffer vertexArray, FloatBuffer normalArray) {
        PMXTransformingModel ptm = modelSet.get(model);
        vertexArray.put(ptm.vertexBuffer);
        normalArray.put(ptm.normalBuffer);
        for (int i = 0; i < ptm.thePool.length; i++)
            ptm.thePool[i].lastUpdateTime = System.currentTimeMillis();
        return;
    }

    private class PMXTransformingModel implements Comparable<PMXTransformingModel> {
        private float[] vertexBuffer;
        private float[] normalBuffer;
        private PMXModel pmxModel;
        private PMXTransformThread[] thePool;

        @Override
        public int compareTo(PMXTransformingModel o) {
            return toString().compareTo(o.toString());
        }
    }
}
