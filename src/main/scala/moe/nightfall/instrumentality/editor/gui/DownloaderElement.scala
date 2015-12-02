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
import moe.nightfall.instrumentality.{ModelDownloadTask, ModelCache, RecommendedInfoCache}
import moe.nightfall.instrumentality.RecommendedInfoCache.DownloadableEntry
import moe.nightfall.instrumentality.animations.AnimSet
import moe.nightfall.instrumentality.editor.EditElement
import moe.nightfall.instrumentality.editor.control.{PowerlineContainerElement, TreeviewElement, TreeviewElementStructurer}
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream

import scala.collection.JavaConversions._

/**
 * Automatically downloads and sets up models.
 * Created on 26/09/15.
 */
class DownloaderElement(val rootPanel: PowerlineContainerElement) extends EditElement {
    var listview = new TreeviewElement[DownloadableEntry](new TreeviewElementStructurer[DownloadableEntry] {
        override def getNodeName(n: DownloadableEntry): String = n.name + ":" + n.author

        override def onNodeClick(n: DownloadableEntry) = {
            if (n.download == "") {
                // What to do...?
            } else {
                if (n.downloadDir == "") {
                    Desktop.getDesktop().browse(new URI(n.download))
                } else {
                    val task = new ModelDownloadTask(n, n.sha)
                    val tpe = new TaskProgressElement(rootPanel, DownloaderElement.this, false, task)
                    new Thread(task).start()
                    tpe.addReturnTask((tpe) => {
                        ModelCache.notifyModelsAdded
                        RecommendedInfoCache.refreshAvailable
                    })
                    // override our own settings
                    rootPanel.noCleanupOnChange = true
                    rootPanel.setUnderPanel(tpe, false)
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
