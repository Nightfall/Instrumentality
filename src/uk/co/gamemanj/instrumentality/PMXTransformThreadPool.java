package uk.co.gamemanj.instrumentality;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;

/**
 * Created on 26/07/15.
 */
public class PMXTransformThreadPool {
    public PMXTransformThread[] thePool;
    public PMXTransformThreadPool(int cores) {
        thePool=new PMXTransformThread[cores];
    }
    public void transformModel(PMXModel model, FloatBuffer vertexArray, FloatBuffer normalArray) {
        float[] fA=new float[vertexArray.capacity()];
        float[] fB=new float[normalArray.capacity()];
        for (int i=0;i<thePool.length;i++) {
            // threads don't like restarts for some reason
            thePool[i]=new PMXTransformThread();

            thePool[i].model=model;
            thePool[i].vertexArray=fA;
            thePool[i].normalArray=fB;
            thePool[i].stride=thePool.length;
            thePool[i].ofs=i;

            thePool[i].start();
        }
        for (int i=0;i<thePool.length;i++) {
            try {
                thePool[i].join();
            } catch (InterruptedException e) {
            }
        }
        vertexArray.put(fA);
        normalArray.put(fB);
    }
}
