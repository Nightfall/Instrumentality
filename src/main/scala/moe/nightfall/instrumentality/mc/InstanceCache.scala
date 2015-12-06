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
package moe.nightfall.instrumentality.mc

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue

import moe.nightfall.instrumentality.animations.AnimSet
import moe.nightfall.instrumentality.{Loader, PMXModel}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer

/**
 * A not-thread-safe implementation of a sort-of hashmap, for one specific purpose:
 * We need to know when players are deallocated from memory so we can delete their VBOs.
 */
object InstanceCache {

    // TODO SCALA Use scala classes, ACTUALLY, rewrite all of this using FP
    // NOTE TO WHOEVER WROTE THAT ^ : Whatever you do, the hashmap needs to use weak references in the key field
    private val cacheDivisions: Array[collection.mutable.Set[ModelCacheEntry]] = new Array(0x100)
    private val changes: ConcurrentLinkedQueue[ChangeEntry] = new ConcurrentLinkedQueue[ChangeEntry]

    /**
     * Updates existing players,
     * and removes GL objects used by players whose memory has been freed.
     * This ensures MC is done with the object by the time we free the GL object.
     */
    def update(dt: Double) {
        for (div <- 0 until cacheDivisions.length) {
            if (cacheDivisions(div) != null) {
                val cache = cacheDivisions(div).clone

                // TODO SCALA This is ugly
                val i = cacheDivisions(div).iterator
                while (i.hasNext) {
                    val mce = i.next()
                    if (mce.playerRef.get() == null) {
                        if (mce.value != null)
                            mce.value.cleanupGL()
                        Loader.currentFileListeners -= mce.cfHook
                        cache.remove(mce)
                    } else {
                        if (mce.value != null)
                            mce.value.update(dt)
                    }
                }
                if (cacheDivisions(div).size == 0)
                    cacheDivisions(div) = null
            }
        }
        var ch = Seq[ChangeEntry]()
        while (true) {
            val ce = changes.poll()
            if (ce == null) {
                ch.foreach((c) => changes.add(c))
                return
            }
            val pent = Minecraft.getMinecraft.theWorld.getPlayerEntityByName(ce.changeTarget)
            if (pent == null) {
                // NOTE: Forge's magical stacktrace thing only works with the functions directly
                System.err.println("Could not apply change to \"" + ce.changeTarget + "\" : entity does not exist")
                ch = ch :+ ce
            } else {
                setModel(pent, ce.newModel, ce.newAnims)
            }
        }
    }

    /**
     * @param player The EntityPlayer to apply a model to.
     * @param model  The model to apply.
     * @return The MCE(Note that MCEs are reused - they are always assigned to the same EntityPlayer, though.)
     */
    def setModel(player: EntityPlayer, model: PMXModel, animSet: AnimSet): ModelCacheEntry = {
        val div = player.hashCode() & 0xFF00 >> 8
        var dll = cacheDivisions(div)
        if (dll == null) {
            dll = collection.mutable.Set()
            cacheDivisions(div) = dll
        }

        var nm = dll.find(_.playerRef.get == player).orNull
        if (nm == null) {
            nm = new ModelCacheEntry()
            nm.playerRef = new WeakReference(player)
            dll += nm
        } else {
            if (nm.value != null) {
                nm.value.cleanupGL()
                nm.value = null
            }
        }

        if (model == null)
            return nm

        // This is the only case where nm.value ends up != null
        nm.value = new PlayerInstance(model, animSet)
        return nm
    }

    // TODO SCALA Return option
    def getModel(player: EntityPlayer): ModelCacheEntry = {
        val div = player.hashCode() & 0xFF00 >> 8
        var dll = cacheDivisions(div)
        if (dll == null)
            dll = collection.mutable.Set()
        cacheDivisions(div) = dll

        return dll.find(_.playerRef.get == player) orNull
    }

    // This is how MT writes are done
    def queueChange(user: EntityPlayer, pm: PMXModel, as: AnimSet) {
        queueChange(user.getCommandSenderName, pm, as)
    }

    def queueChange(user: String, pm: PMXModel, as: AnimSet) {
        val ce = new ChangeEntry(user, pm, as)
        changes.add(ce)
    }
}

class ModelCacheEntry {
    var value: PlayerInstance = null
    // EntityPlayer. Don't keep not-weak references long-term anywhere in the code.
    var playerRef: WeakReference[EntityPlayer] = null
    // If != null, is the currentFileListeners Runnable. Is removed from there when the MCE is removed.
    var cfHook: () => Unit = null
}

private class ChangeEntry(
                           // Uses a string because it can be called from another thread.
                           // Hopefully people can't change name while connected to a server,
                           // if this is wrong, then change this to a GUID.
                           // getCommandSenderName is used, since that's what the world uses to find a player
                           val changeTarget: String,
                           val newModel: PMXModel,
                           val newAnims: AnimSet
                             )
