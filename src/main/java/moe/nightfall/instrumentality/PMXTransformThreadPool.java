/*
 * Copyright (c) 2015, Nightfall Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package moe.nightfall.instrumentality;

import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 26/07/15.
 */
public class PMXTransformThreadPool {
	public ConcurrentHashMap<PMXInstance, PMXTransformingModel> modelSet = new ConcurrentHashMap<PMXInstance, PMXTransformingModel>();
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
	 * @param cpm
	 *            Amount of threads to create.
	 * @param monitor
	 *            If this thread dies, the threadpool assumes the program has
	 *            died, and cleans up after itself.
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

	public void addModel(PMXInstance pm) {
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
	public void transformModel(PMXInstance model, FloatBuffer vertexArray, FloatBuffer normalArray) {
		PMXTransformingModel ptm = modelSet.get(model);
		vertexArray.put(ptm.vertexBuffer);
		normalArray.put(ptm.normalBuffer);
		ptm.lastUpdateTime = System.currentTimeMillis();
		return;
	}

	public class PMXTransformingModel implements Comparable<PMXTransformingModel> {
		public float[] vertexBuffer;
		public float[] normalBuffer;
		public PMXInstance pmxModel;
		public long lastUpdateTime;

		@Override
		public int compareTo(PMXTransformingModel o) {
			return toString().compareTo(o.toString());
		}
	}
}
