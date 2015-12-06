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
package moe.nightfall.instrumentality.mc.network

import java.io._

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}
import io.netty.buffer.ByteBuf
import moe.nightfall.instrumentality.animations.AnimSet
import moe.nightfall.instrumentality.mc.{ClientProxy, MikuMikuCraft}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound

import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap

class SendSHAMessage extends IMessage {
    // MEANINGS:
    // Sent to server, player is ignored, and dataManifest is the sender's DM.
    // Sent to client, player is the player it's coming from, and dataManifest is the DM.
    // If the server is trying to get you to resend (server does no caching of DMs) then player is "" and DM is None
    // Note that a copy of the AnimSet is sent with the DM, since:
    // AnimSets can be updated often
    // Two people may use different AnimSets
    // The client probably doesn't want their copy updated unless they didn't have the model anyways (hence it's also in the DM)
    var dataManifest: Option[(NBTTagCompound, AnimSet)] = None
    var player = ""

    override def toBytes(byteBuf: ByteBuf): Unit = {
        byteBuf.writeBoolean(dataManifest.isDefined)
        if (dataManifest.isDefined) {
            val data = new ByteArrayOutputStream()
            dataManifest.get._2.save(new DataOutputStream(data))
            val dataArray = data.toByteArray

            ByteBufUtils.writeTag(byteBuf, dataManifest.get._1)
            byteBuf.writeInt(dataArray.length)
            byteBuf.writeBytes(dataArray)
        }
        ByteBufUtils.writeUTF8String(byteBuf, player)
    }

    override def fromBytes(byteBuf: ByteBuf): Unit = {
        val exists = byteBuf.readBoolean()
        dataManifest = None
        if (exists) {
            val tag = ByteBufUtils.readTag(byteBuf)
            val data = new Array[Byte](byteBuf.readInt())
            byteBuf.readBytes(data)

            var animSet = new AnimSet()
            try {
                animSet.load(new DataInputStream(new ByteArrayInputStream(data)))
            } catch {
                case iOException: IOException => {
                    animSet = new AnimSet()
                }
            }
            dataManifest = Some((tag, animSet))
        }
        player = ByteBufUtils.readUTF8String(byteBuf)
    }
}

class SendSHAMessageClientHandler extends IMessageHandler[SendSHAMessage, IMessage] {
    override def onMessage(req: SendSHAMessage, messageContext: MessageContext): IMessage = {
        if (Minecraft.getMinecraft.thePlayer != null) {
            if (req.player == Minecraft.getMinecraft.thePlayer.getCommandSenderName) {
                return null
            }
        }
        if (req.player == "") {
            // Server wants us to reply with our data
            return ClientProxy.getSelfSHA
        }
        // Might run in another thread on 1.8
        var h = HashMap[String, String]()
        var hb: AnimSet = null
        if (req.dataManifest.isDefined) {
            var keys = req.dataManifest.get._1.func_150296_c() // gets the set of keys!!!
            hb = req.dataManifest.get._2
            keys.foreach(a => h += a.asInstanceOf[String] -> req.dataManifest.get._1.getString(a.asInstanceOf[String]))
        } else {
            h = null // yeah, IK, "use Option"...
        }
        ClientProxy.updateRemoteModel(req.player, h, hb)
        null
    }
}

class SendSHAMessageServerHandler extends IMessageHandler[SendSHAMessage, IMessage] {
    override def onMessage(req: SendSHAMessage, messageContext: MessageContext): IMessage = {
        if (messageContext.getServerHandler.playerEntity != null) {
            req.player = messageContext.getServerHandler.playerEntity.getCommandSenderName // forceful override
        } else {
            null
        }
        if (req.dataManifest.isDefined) {
            val kv = req.dataManifest.get._1
            kv.func_150296_c().foreach((h) => {
                MikuMikuCraft.proxy.serverHashOwners += kv.getString(h.asInstanceOf[String]) -> messageContext.getServerHandler.playerEntity
            })
        }
        MikuMikuCraft.proxy.serverKnownDataManifests += req.player -> req
        messageContext.getServerHandler.playerEntity.worldObj.playerEntities.foreach((e) => {
            if (messageContext.getServerHandler.playerEntity != e) {
                System.err.println("Sending immediate update to other players in same world: " + e.asInstanceOf[EntityPlayerMP].getCommandSenderName)
                MikuMikuCraft.mikuNet.sendTo(req, e.asInstanceOf[EntityPlayerMP])
            }
        })
        null
    }
}
