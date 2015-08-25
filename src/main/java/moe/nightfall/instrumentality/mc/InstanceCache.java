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
package moe.nightfall.instrumentality.mc;

import moe.nightfall.instrumentality.*;
import net.minecraft.entity.player.EntityPlayer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A not-thread-safe implementation of a sort-of hashmap, for one specific purpose:
 * We need to know when players are deallocated from memory so we can delete their VBOs.
 */
public final class InstanceCache {

    private InstanceCache() {

    }

    private static LinkedList<ModelCacheEntry> cacheDivisions[] = new LinkedList[0x100];

    /**
     * Updates existing players,
     * and removes GL objects used by players whose memory has been freed.
     * This ensures MC is done with the object by the time we free the GL object.
     */
    public static void update(double dT) {
        for (int div = 0; div < cacheDivisions.length; div++) {
            if (cacheDivisions[div] == null)
                continue;
            Iterator<ModelCacheEntry> i = cacheDivisions[div].iterator();
            while (i.hasNext()) {
                ModelCacheEntry mce = i.next();
                if (mce.playerRef.get() == null) {
                    mce.value.cleanupGL();
                    Loader.currentFileListeners.remove(mce.cfHook);
                    i.remove();
                } else {
                    mce.value.update(dT);
                }
            }
            if (cacheDivisions[div].size() == 0)
                cacheDivisions[div] = null;
        }
    }

    /**
     * @param player The EntityPlayer to apply a model to.
     * @param model  The model to apply.
     * @return The MCE(Note that MCEs are reused - they are always assigned to the same EntityPlayer, though.)
     */
    public static ModelCacheEntry setModel(EntityPlayer player, PMXModel model) {
        int div = player.hashCode() & 0xFF00 >> 8;
        LinkedList<ModelCacheEntry> dll = cacheDivisions[div];
        if (dll == null)
            cacheDivisions[div] = dll = new LinkedList<ModelCacheEntry>();
        ModelCacheEntry nm = null;
        for (ModelCacheEntry mce : dll)
            if (mce.playerRef.get() == player) {
                nm = mce;
                break;
            }

        if (nm == null) {
            nm = new ModelCacheEntry();
            nm.playerRef = new WeakReference<EntityPlayer>(player);
            dll.add(nm);
        } else {
            if (nm.value != null) {
                nm.value.cleanupGL();
                nm.value = null;
            }
        }

        if (model == null)
            return nm;
        // This is the only case where nm.value ends up != null
        nm.value = new PlayerInstance(model);
        return nm;
    }

    public static ModelCacheEntry getModel(EntityPlayer player) {
        int div = player.hashCode() & 0xFF00 >> 8;
        LinkedList<ModelCacheEntry> dll = cacheDivisions[div];
        if (dll == null)
            cacheDivisions[div] = dll = new LinkedList<ModelCacheEntry>();
        // Try to find the value
        for (ModelCacheEntry mce : dll)
            if (mce.playerRef.get() == player)
                return mce;
        return null;
    }

    public static class ModelCacheEntry {
        PlayerInstance value;
        // EntityPlayer. Don't keep not-weak references long-term anywhere in the code.
        WeakReference<EntityPlayer> playerRef;
        // If != null, is the currentFileListeners Runnable. Is removed from there when the MCE is removed.
        Runnable cfHook;
    }
}
