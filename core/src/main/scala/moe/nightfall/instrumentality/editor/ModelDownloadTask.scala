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
package moe.nightfall.instrumentality.editor

import java.io.{File, FileInputStream, FileOutputStream, IOException}
import java.net.URL
import java.util.zip.{ZipEntry, ZipInputStream}

import moe.nightfall.instrumentality.ModelCache.{DownloadingPMXFilenameLocator, IPMXFilenameLocator}
import moe.nightfall.instrumentality.RecommendedInfoCache.DownloadableEntry
import moe.nightfall.instrumentality.{ModelCache}

/**
 * Used for downloading models by DownloaderElement.
 * Created on 09/10/15.
 */
class ModelDownloadTask(val n: DownloadableEntry, val downloadName: String) extends MeasurableTask with Runnable {

    // Scala is magic...
    var generalTask = "Starting download thread..."
    var subTask = ""
    var progress = 0.0d
    var state = TaskState.Prestart

    override def run(): Unit = {
        progress = 0
        generalTask = "Downloading ZIP..."
        subTask = n.download
        state = TaskState.Running
        val fakeIt = false
        try {
            if (fakeIt) {
                Thread.sleep(2500)
                progress = 0.5d
                Thread.sleep(2500)
                progress = 1
            } else {
                val fakeIt2 = false
                var f = new File("nodos.zip")
                if (!fakeIt2) {
                    val uc = new URL(n.download).openConnection()
                    uc.addRequestProperty("User-Agent", "MikuMikuCraft")
                    uc.connect()
                    val contentLength = uc.getContentLengthLong
                    f = File.createTempFile("MMCdl", ".dat")
                    val fos = new FileOutputStream(f)
                    val fis = uc.getInputStream
                    var todoLen = contentLength
                    val buffer = new Array[Byte](1024)
                    while (todoLen > 0) {
                        progress = 0.5d - (todoLen / (contentLength * 2d))
                        var readLen = fis.read(buffer)
                        // Not sure if this case will ever happen, but just to be sure...
                        if (readLen < 0)
                            throw new IOException("Download failure")
                        todoLen -= readLen
                        fos.write(buffer, 0, readLen)
                    }
                    fis.close()
                    fos.close()
                } else {
                    progress = 0.5d
                    Thread.sleep(1000)
                }
                generalTask = "Loading ZIP..."
                val fis = new FileInputStream(f)
                val zais = new ZipInputStream(fis)
                var za = Seq[ZipEntry]()
                var entry: ZipEntry = zais.getNextEntry
                while (entry != null) {
                    println(entry.getName)
                    za :+= entry
                    zais.closeEntry()
                    entry = zais.getNextEntry
                }
                fis.close()

                var root = n.downloadDir.toLowerCase
                if (root == "/")
                    root = ""
                var fileProgress = 0.1d
                val filenameLocator = new IPMXFilenameLocator {
                    override def listFiles(): Seq[String] = {
                        return za.filter(_.getName.toLowerCase.startsWith(root))
                            .filter(!_.isDirectory)
                            .map(af => af.getName.toLowerCase.substring(root.length))
                    }

                    override def apply(filename: String): Array[Byte] = {
                        subTask = filename
                        progress += fileProgress
                        val s = root + filename.toLowerCase.replace('\\', '/')
                        // Ok, now "seek"
                        val fis = new FileInputStream(f)
                        val zais = new ZipInputStream(fis)
                        var entry: ZipEntry = zais.getNextEntry
                        while (entry != null) {
                            if (entry.getName.toLowerCase.equals(s)) {
                                // OK
                                val buf = new Array[Byte](entry.getSize.toInt)
                                var remaining = buf.length
                                while (remaining > 0) {
                                    val len = zais.read(buf, buf.length - remaining, remaining)
                                    if (len < 0)
                                        throw new IOException("Did not read whole file")
                                    remaining -= len
                                }
                                fis.close()
                                return buf
                            }
                            zais.closeEntry()
                            entry = zais.getNextEntry
                        }
                        fis.close()
                        throw new IOException("Could not find a file")
                    }
                }
                fileProgress = 0.5d / filenameLocator.listFiles().size
                val name = ModelCache.findFreeName(downloadName)
                ModelCache.getInternal(new DownloadingPMXFilenameLocator(filenameLocator, name, -1), name, true)
            }
            state = TaskState.Success
        } catch {
            case e: IOException => {
                generalTask = subTask + " : failed"
                subTask = e.toString
                state = TaskState.Failure
            }
        }
    }

}
