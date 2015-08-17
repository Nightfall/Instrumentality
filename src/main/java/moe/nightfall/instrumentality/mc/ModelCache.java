package moe.nightfall.instrumentality.mc;

import moe.nightfall.instrumentality.Main;
import net.minecraft.entity.player.EntityPlayer;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A not-thread-safe implementation of a sort-of hashmap, for one specific purpose:
 * We need to know when players are deallocated from memory so we can delete their VBOs.
 */
public class ModelCache {

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
                    i.remove();
                } else {
                    mce.value.update(dT);
                }
            }
            if (cacheDivisions[div].size() == 0)
                cacheDivisions[div] = null;
        }
    }

    public static PlayerModel getModel(EntityPlayer player) {
        int div = player.hashCode() & 0xFF00 >> 4;
        LinkedList<ModelCacheEntry> dll = cacheDivisions[div];
        if (dll == null)
            cacheDivisions[div] = dll = new LinkedList<ModelCacheEntry>();
        // Try to find the value
        for (ModelCacheEntry mce : dll)
            if (mce.playerRef.get() == player)
                return mce.value;
        // Try to create a new value
        ModelCacheEntry nm = new ModelCacheEntry();
        nm.playerRef = new WeakReference<EntityPlayer>(player);
        nm.value = new PlayerModel(Main.pf, 12);
        dll.add(nm);
        return nm.value;
    }

    private static class ModelCacheEntry {
        PlayerModel value;
        WeakReference<EntityPlayer> playerRef;
    }
}
