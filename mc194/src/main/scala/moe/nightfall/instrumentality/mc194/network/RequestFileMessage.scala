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
package moe.nightfall.instrumentality.mc194.network

import java.io._

import io.netty.buffer.ByteBuf
import moe.nightfall.instrumentality.mc194.{ClientProxy, MikuMikuCraft}
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Lock

class RequestFileMessage extends IMessage {
    // MEANINGS:
    // Sent to server, it means the server should give the client the file.
    // Sent to client, it means the server needs a file from the client.
    // If fileData is Some, then it's a response.
    var fileHash = ""
    var fileData: Option[Array[Byte]] = None
    // Used when sending a file.
    var seqNum = 0
    var seqSize = 0

    override def toBytes(byteBuf: ByteBuf): Unit = {
        byteBuf.writeBoolean(fileData.isDefined)
        if (fileData.isDefined) {
            byteBuf.writeInt(seqNum)
            byteBuf.writeInt(seqSize)
            byteBuf.writeInt(fileData.get.length)
            byteBuf.writeBytes(fileData.get)
        }
        ByteBufUtils.writeUTF8String(byteBuf, fileHash)
    }

    override def fromBytes(byteBuf: ByteBuf): Unit = {
        val exists = byteBuf.readBoolean()
        fileData = None
        if (exists) {
            seqNum = byteBuf.readInt()
            seqSize = byteBuf.readInt()
            fileData = Some(new Array[Byte](byteBuf.readInt()))
            byteBuf.readBytes(fileData.get)
        }
        fileHash = ByteBufUtils.readUTF8String(byteBuf)
    }
}

class RequestFileMessageClientHandler extends IMessageHandler[RequestFileMessage, IMessage] {
    override def onMessage(req: RequestFileMessage, messageContext: MessageContext): IMessage = {
        if (req.fileData.isEmpty) {
            val hasData = ClientProxy.hashToFile.get(req.fileHash)
            if (hasData.isDefined) {
                val fis = new FileInputStream(hasData.get)
                val list = new ListBuffer[RequestFileMessage]
                var data = new Array[Byte](0x7000) // leave 4096 bytes for Forge data?
                var size = fis.read(data)
                var num = 0
                while (size > 0) {
                    val pkt = new RequestFileMessage()
                    pkt.fileHash = req.fileHash
                    pkt.fileData = Some(data.slice(0, size))
                    pkt.seqNum = num
                    num += 1
                    list += pkt
                    size = fis.read(data)
                }
                fis.close()
                list.foreach((pkt) => {
                    pkt.seqSize = num
                    MikuMikuCraft.mikuNet.sendToServer(pkt)
                })
                System.out.println("Server wanted file " + req.fileHash)
            } else {
                System.err.println("Server wanted file " + req.fileHash + ", but we don't have that!")
            }
        } else {
            MikuMikuCraft.proxy.onDataReceive(req, false)
        }
        null
    }
}

class RequestFileMessageServerHandler extends IMessageHandler[RequestFileMessage, IMessage] {
    override def onMessage(req: RequestFileMessage, messageContext: MessageContext): IMessage = {
        if (req.fileData.isEmpty) {
            // Someone wants some data - pass it along, bring back the response later!
            val potentialData = MikuMikuCraft.proxy.serverCache.get(req.fileHash)
            if (potentialData != null) {
                potentialData._3.foreach((rfm) => {
                    MikuMikuCraft.mikuNet.sendTo(rfm, messageContext.getServerHandler.playerEntity)
                })
                return null
            }
            val targ = MikuMikuCraft.proxy.serverHashOwners.get(req.fileHash)
            var seq: Array[RequestFileMessage] = null
            var seqLock: Lock = new Lock
            MikuMikuCraft.proxy.serverCallbackOnData.put((req.fileHash, (pkt: RequestFileMessage) => {
                // Send the packet to the next person, yay!
                MikuMikuCraft.mikuNet.sendTo(pkt, messageContext.getServerHandler.playerEntity)
                // TODO: Adjust this. Right now: 32kb * 1024 == 32MB.
                if ((pkt.seqSize > 1024) || (pkt.seqSize < 0)) {
                    // This person's playing a joke on us
                    true
                } else {
                    seqLock.acquire()
                    if (seq == null)
                        seq = new Array[RequestFileMessage](pkt.seqSize)
                    if ((pkt.seqNum < 0) || (pkt.seqNum >= seq.length)) {
                        seqLock.release()
                        true
                    } else {
                        seq(pkt.seqNum) = pkt
                        val result = seq.filter(_ == null).size == 0
                        if (result) {
                            var totalSize = 0
                            seq.foreach((a) => totalSize += a.fileData.get.length)

                            // Sort by age
                            var l = MikuMikuCraft.proxy.serverCache.toList.sortWith((a, b) => a._2._1 > b._2._1)
                            var allSize = 0
                            l.foreach((kv) => allSize += kv._2._2 + 256)
                            System.out.println("MMC Cache: Entering with antispam padded size: " + allSize)
                            // allSize now contains the total size of this snapshot of the cache.
                            // To avoid spam, an overhead of 256 bytes is added per file.
                            // If needed, start deleting items
                            while ((allSize + totalSize > MikuMikuCraft.proxy.serverCacheLimit) && (l.length > 0)) {
                                val deleting = l.head
                                l = l.tail
                                allSize -= deleting._2._2 + 256
                                System.out.println("MMC Cache: Deleting " + deleting._1 + " (" + deleting._2._2 + ") to make room for " + pkt.fileHash + " (total including antispam padding: " + allSize + ")")
                                MikuMikuCraft.proxy.serverCache.remove(deleting._1)
                            }
                            MikuMikuCraft.proxy.serverCache.put(pkt.fileHash, (System.currentTimeMillis(), totalSize, seq))
                            System.out.println("MMC Cache: Adding " + pkt.fileHash + " (" + totalSize + ") (total including antispam padding: " + (allSize + totalSize) + ")")
                        }
                        seqLock.release()
                        result
                    }
                }
            }), Unit)
            if (targ.isDefined) {
                MikuMikuCraft.mikuNet.sendTo(req, targ.get)
                System.out.println("Client wanted file " + req.fileHash)
            } else {
                System.err.println("Client wanted file " + req.fileHash + ", but nobody has that!")
            }
        } else {
            MikuMikuCraft.proxy.onDataReceive(req, true)
        }
        null
    }
}
