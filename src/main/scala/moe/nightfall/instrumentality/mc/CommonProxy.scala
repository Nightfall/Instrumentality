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

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import moe.nightfall.instrumentality.mc.network._
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.event.entity.EntityJoinWorldEvent

//import moe.nightfall.instrumentality.mc.network.SendSHAMessage
import net.minecraftforge.common.MinecraftForge

import scala.collection.JavaConversions._

//,WantDataMessage,GiveDataMessage}

class CommonProxy {

    // NOTE: The two of these are a potential memory leak.
    //       Basically - pray they aren't too much of a memory leak.
    val serverKnownDataManifests = collection.concurrent.TrieMap[String, SendSHAMessage]()
    val serverHashOwners = collection.concurrent.TrieMap[String, EntityPlayerMP]()

    // The Long is the currentTimeMillis upon which the data should be deleted when new data comes in.
    // The Int is the total size.
    // Needs to be iterable so data can be removed when it's too old.
    val serverCache = new java.util.concurrent.ConcurrentHashMap[String, (Long, Int, Array[RequestFileMessage])]()

    // 4MiB cache limit.

    val serverCacheLimit = 4 * 1024 * 1024

    // These callbacks can run from basically any thread
    // If the callback returns true, it's removed.
    var clientCallbackOnData = new java.util.concurrent.ConcurrentHashMap[(String, (RequestFileMessage) => Boolean), Unit]()
    var serverCallbackOnData = new java.util.concurrent.ConcurrentHashMap[(String, (RequestFileMessage) => Boolean), Unit]()

    def onDataReceive(requestFileMessage: RequestFileMessage, server: Boolean): Unit = {
        val callbacks = (if (server) serverCallbackOnData else clientCallbackOnData)
        callbacks.filter(a => a._1._1 == requestFileMessage.fileHash).foreach((e) => {
            if (requestFileMessage.fileData.isDefined)
                if (e._1._2(requestFileMessage))
                    callbacks.remove(e._1)
        })
    }

    def preInit() {
        MinecraftForge.EVENT_BUS.register(this)
        FMLCommonHandler.instance.bus.register(this)
        // sent by client using send-to-server to say "yes I support MMC",
        // and to send the current model's SHA. Note that sending a SHA indicates to the server you have that SHA
        MikuMikuCraft.mikuNet.registerMessage(classOf[SendSHAMessageServerHandler], classOf[SendSHAMessage], 0, Side.SERVER)
        MikuMikuCraft.mikuNet.registerMessage(classOf[SendSHAMessageClientHandler], classOf[SendSHAMessage], 1, Side.CLIENT)
        MikuMikuCraft.mikuNet.registerMessage(classOf[RequestFileMessageServerHandler], classOf[RequestFileMessage], 2, Side.SERVER)
        MikuMikuCraft.mikuNet.registerMessage(classOf[RequestFileMessageClientHandler], classOf[RequestFileMessage], 3, Side.CLIENT)
    }

    @SubscribeEvent
    def onEntityJoin(entityJoinWorldEvent: EntityJoinWorldEvent) {
        if (!entityJoinWorldEvent.world.isRemote) {
            // We're on server
            if (entityJoinWorldEvent.entity.isInstanceOf[EntityPlayerMP]) {
                // Forward all the SendSHA messages we know of in that world to the player
                System.out.println("forwarding known SHAs to: " + entityJoinWorldEvent.entity.getCommandSenderName);
                entityJoinWorldEvent.world.playerEntities.foreach((e) => {
                    val d = serverKnownDataManifests.get(e.asInstanceOf[EntityPlayerMP].getCommandSenderName)
                    if (d.isDefined)
                        MikuMikuCraft.mikuNet.sendTo(d.get, e.asInstanceOf[EntityPlayerMP])
                })
            }
        }
    }


}
