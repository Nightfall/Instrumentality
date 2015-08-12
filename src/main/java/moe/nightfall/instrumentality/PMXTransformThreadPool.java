package moe.nightfall.instrumentality;

import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 26/07/15.
 */
public class PMXTransformThreadPool {
    public ConcurrentHashMap<PMXModel, PMXTransformingModel> modelSet = new ConcurrentHashMap<PMXModel, PMXTransformingModel>();
    public PMXTransformThread[] threads;

    /**
     * Keeps the threads alive.
     */
    public void keepAlive() {
        for (int i = 0; i < threads.length; i++)
            threads[i].lastUpdate = System.currentTimeMillis();
    }

    /**
     * Tells the threads to die ASAP.
     */
    public void killSwitch() {
        for (int i = 0; i < threads.length; i++)
            threads[i].killSwitch = true;
    }

    /**
     * Creates a threadpool.
     *
     * @param cpm     Amount of threads to create.
     * @param monitor If this thread dies, the threadpool assumes the program has died, and cleans up after itself.
     */
    public PMXTransformThreadPool(int cpm, Thread monitor) {
        threads = new PMXTransformThread[cpm];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new PMXTransformThread();
            threads[i].ofs = i;
            threads[i].stride = threads.length;
            threads[i].pttp = this;
            threads[i].lastUpdate = System.currentTimeMillis();
            threads[i].monitorThread = monitor;
            threads[i].setPriority(Thread.MIN_PRIORITY);
            threads[i].start();
        }
    }

    public void addModel(PMXModel pm) {
        PMXTransformingModel ptm = new PMXTransformingModel();
        ptm.pmxModel = pm;
        ptm.vertexBuffer = new float[pm.theFile.vertexData.length * 3];
        ptm.normalBuffer = new float[pm.theFile.vertexData.length * 3];
        ptm.lastUpdateTime = System.currentTimeMillis();
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
        ptm.lastUpdateTime = System.currentTimeMillis();
        return;
    }

    public class PMXTransformingModel implements Comparable<PMXTransformingModel> {
        public float[] vertexBuffer;
        public float[] normalBuffer;
        public PMXModel pmxModel;
        public long lastUpdateTime;

        @Override
        public int compareTo(PMXTransformingModel o) {
            return toString().compareTo(o.toString());
        }
    }
}
