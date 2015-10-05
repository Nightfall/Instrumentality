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
package moe.nightfall.instrumentality.editor.gui

import java.awt.Desktop
import java.io.{FileInputStream, File, FileOutputStream, IOException}
import java.net.{URI, URL}
import java.util.zip.{ZipInputStream, ZipEntry, ZipFile}

import moe.nightfall.instrumentality.ModelCache.{DownloadingPMXFilenameLocator, IPMXFilenameLocator}
import moe.nightfall.instrumentality.{ModelCache, RecommendedInfoCache}
import moe.nightfall.instrumentality.RecommendedInfoCache.DownloadableEntry
import moe.nightfall.instrumentality.animations.PoseSet
import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control.{ButtonBarContainerElement, TreeviewElement, TreeviewElementStructurer}
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream

import scala.collection.JavaConversions._

/**
 * Automatically downloads and sets up models.
 * Created on 26/09/15.
 */
class DownloaderElement(val rootPanel: ButtonBarContainerElement) extends EditElement {
    var listview = new TreeviewElement[DownloadableEntry](new TreeviewElementStructurer[DownloadableEntry] {
        override def getNodeName(n: DownloadableEntry): String = n.name + ":" + n.author

        override def onNodeClick(n: DownloadableEntry) = {
            val ps = new PoseSet
            ps.loadForHash(n.sha)
            if (ps.downloadURL == "") {

            } else {
                if (ps.downloadBaseFolder == "") {
                    Desktop.getDesktop().browse(new URI(ps.downloadURL))
                } else {
                    val tpe = new TaskProgressElement(rootPanel, DownloaderElement.this, false)
                    tpe.label.text = "Downloading " + ps.downloadURL + "..."
                    tpe.addReturnTask((tpe) => {
                        ModelCache.notifyModelsAdded
                        RecommendedInfoCache.refreshAvailable
                    })
                    // override our own settings
                    rootPanel.noCleanupOnChange = true
                    rootPanel.setUnderPanel(tpe, false)
                    // Testing settings:
                    // fakeIt: No-Op the model installation process
                    // fakeIt2: No-Op the download (to avoid hitting servers)
                    //          (Installation still proceeds)
                    val fakeIt = false
                    new Thread() {
                        override def run() = {
                            try {
                                if (fakeIt) {
                                    Thread.sleep(5000)
                                } else {
                                    val fakeIt2 = false
                                    var f = new File("nodos.zip")
                                    if (!fakeIt2) {
                                        val uc = new URL(ps.downloadURL).openConnection()
                                        uc.addRequestProperty("User-Agent", "MikuMikuCraft")
                                        uc.connect()
                                        val contentLength = uc.getContentLengthLong
                                        f = File.createTempFile("MMCdl", ".dat")
                                        val fos = new FileOutputStream(f)
                                        val fis = uc.getInputStream
                                        var todoLen = contentLength
                                        val buffer = new Array[Byte](1024)
                                        while (todoLen > 0) {
                                            var readLen = fis.read(buffer)
                                            // Not sure if this case will ever happen, but just to be sure...
                                            if (readLen < 0)
                                                throw new IOException("Download failure")
                                            todoLen -= readLen
                                            fos.write(buffer, 0, readLen)
                                        }
                                        fis.close()
                                        fos.close()
                                    }
                                    tpe.label.text = "Loading ZIP..."
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

                                    var root = ps.downloadBaseFolder.toLowerCase
                                    if (root == "/")
                                        root = ""

                                    val filenameLocator = new IPMXFilenameLocator {
                                        override def listFiles(): Seq[String] = {
                                            return za.filter(_.getName.toLowerCase.startsWith(root))
                                                .filter(!_.isDirectory)
                                                .map(af => af.getName.toLowerCase.substring(root.length))
                                        }

                                        override def apply(filename: String): Array[Byte] = {
                                            tpe.label.text = filename
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
                                    ModelCache.getInternal(new DownloadingPMXFilenameLocator(filenameLocator, n.sha, -1), n.sha, true)
                                }
                                // listview will relayout upon being moved
                                tpe.returnPlease = true
                            } catch {
                                case e: IOException => tpe.label.text += "\nFailed: " + e.toString
                            }
                        }
                    }.start()
                }
            }
        }

        override def getChildNodes(n: Option[DownloadableEntry]): Iterable[DownloadableEntry] = {
            if (n.isEmpty)
                return RecommendedInfoCache.availableEntries
            Seq.empty[DownloadableEntry]
        }
    })
    subElements += listview

    override def layout(): Unit = {
        listview.setSize(width, height)
    }

}
