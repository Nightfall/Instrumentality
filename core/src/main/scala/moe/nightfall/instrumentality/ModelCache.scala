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
package moe.nightfall.instrumentality

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, DataInputStream, File, FileInputStream, FileOutputStream, IOException}
import javax.imageio.ImageIO

import com.google.common.hash.Hashing
import moe.nightfall.instrumentality.animations.AnimSet

/**
 * Model cache. This is designed to be used from multiple threads, as long as
 * you do all drawing on the same thread (drawing triggers VBO building and
 * other operations that can't be multithreaded) Created on 19/08/15.
 */
object ModelCache {

    // Maximum total usage. Setting this to 0 effectively disables remote server
    // downloading.
    // Setting this to -1 means "unlimited" (NOT RECOMMENDED : this makes it a
    // lot easier for a random player to spam your bandwidth away)
    var maxTotalUsage = -1L

    // Local model repository.
    // This string, and directories directly under it (but not
    // files/subdirectories within those) should NOT be lowercased.
    // Also, it must end in /
    var modelRepository = "mdl/"

    private val localModels = collection.concurrent.TrieMap[String, PMXModel]()
    private val localFromHashCache = collection.concurrent.TrieMap[String, Option[String]]()

    def notifyModelsAdded = localFromHashCache.filter(_._2.isEmpty).foreach(kv => localFromHashCache.remove(kv._1))

    def findPMX(keys: Iterable[String]): String = keys.find(_.toLowerCase.endsWith(".pmx")).getOrElse("mdl.pmx")

    def localFromHash(hash: String): Option[String] = {
        if (localFromHashCache.contains(hash))
            return localFromHashCache(hash)
        val v = {
            getLocalModels() foreach { s =>
                val dm = createManifestForLocal(s)
                if (hashDataManifest(dm.filesToHashes.toMap[String, String]) == hash)
                    return Some(s)
            }
            None
        }
        localFromHashCache(hash) = v
        v
    }

    def findFreeName(prefer: String): String = {
        if (new File(modelRepository + prefer).exists()) {
            var i = 0
            while ((i < 1000) && (new File(modelRepository + prefer + "-" + i).exists()))
                i += 1
            return prefer + "-" + i
        }
        prefer
    }

    /**
     * Gets a PMXModel from a data manifest. Will try local FS, then try server
     *
     * @param hashMap
     * The core DataManifest.
     * @param remoteServer
     *  The remote server should a local copy be unavailable (can be
      * null, but will mean it won't be able to retrieve it)
     * @return The resulting model
     */
    def getByManifest(hashMap: Map[String, String], remoteServer: IPMXLocator): PMXModel = {
        // Check for eligible candidates locally
        val targetHash = hashDataManifest(hashMap)
        println("MMC getByManifest hash: " + targetHash)
        val local = localFromHash(targetHash)
        if (local.isDefined)
            return getLocal(local.get)

        if (remoteServer == null)
            return null

        try {
            val manifestGetter = new IPMXFilenameLocator() {
                override def listFiles(): Seq[String] = hashMap.keys.toSeq
                override def apply(filename: String): Array[Byte] = {
                    val hash = hashMap.get(filename) getOrElse null
                    if (hash == null)
                        throw new IOException("No file " + filename)
                    return remoteServer.getData(hash)
                }
            }

            val data = getInternal(new DownloadingPMXFilenameLocator(manifestGetter, findFreeName(targetHash), maxTotalUsage), targetHash, true)
            if (data != null)
                ModelCache.notifyModelsAdded
            return data
        } catch {
            case e: IOException => {
                System.err.println("getByManifest failed download from server. Reason:")
                e.printStackTrace()
                return null
            }
        }
    }

    def getLocalModels(): Seq[String] = {
        val out = new File(modelRepository).listFiles()
        if (out == null)
            return Seq()
        return out.filter(_.isDirectory()).map(_.getName)
    }

    def getLocal(name: String): PMXModel = {
        val mdl = localModels.get(name)
        if (mdl.isDefined) return mdl.get
        try {
            return getInternal(new FilePMXFilenameLocator(modelRepository + name + "/"), name, false)
        } catch {
            case e: Exception => {
                e.printStackTrace()
                return null
            }
        }
    }

    // getTxt will get .txt files for legal purposes (but will not fail if they cannot be downloaded)
    def getInternal(locator: IPMXFilenameLocator, name: String, getTxt: Boolean): PMXModel = {
        val pmxData = locator(findPMX(locator.listFiles))
        val pmxHash = hashRawBytes(pmxData)
        val pm = new PMXModel(new PMXFile(pmxData), Loader.groupSize)

        if (getTxt) {
            try {
                locator.listFiles.filter(k => k.toLowerCase.endsWith(".txt")).foreach(k => locator(k))
            } catch {
                case _: IOException =>
            }
        }

        try {
            pm.defaultAnims.load(new DataInputStream(new ByteArrayInputStream(locator("mmcposes.dat"))))
        } catch {
            case _: IOException => {
                // default animations are indexed by PMX hash anyway
                pm.defaultAnims.loadForHash(pmxHash)
            }
        }

        loadTextures(pm, pm.theFile, locator)
        if (!localModels.contains(name))
            localModels.put(name, pm)
        return pm
    }

    private def loadTextures(mdl: PMXModel, pf: PMXFile, fl: IPMXFilenameLocator) {
        val mapping = collection.mutable.Map[String, BufferedImage]()
        pf.matData foreach { mat =>
            var str = mat.texTex
            if (str != null) {
                str = str.toLowerCase()
                str = str.replace('\\', '/')

                // It's dumb, but this is the only place arbitrary pathnames can be
                // entered into that we'll accept.
                // So we MUST security-check it. Please, fix this if there is a
                // problem.

                // two dirseps after each other : SUSPICIOUS!
                if (str.matches("//"))
                    throw new IOException("Potentially security-threatening string found (attempt to break into root)");

                // a dirsep at the start of the string: silently remove it
                if (str.matches("^/"))
                    str = str.substring(1)

                // .. : really suspicious!
                if (str.matches("\\.\\."))
                    throw new IOException("Potentially security-threatening string found (attempt to get parent directory)");

                // ./ : just plain weird
                if (str.matches("^\\./"))
                    str = str.substring(2)

                // /./ : wtf
                if (str.matches("/\\./"))
                    throw new IOException("Potentially security-threatening string found (weirdness)");

                try {
                    var bi = mapping.get(str)
                    if (bi.isEmpty) {
                        val img = ImageIO.read(new ByteArrayInputStream(fl(str)))
                        if (img == null)
                            throw new IOException("Could not read image: " + str)
                        bi = Some(img)
                        mapping += str -> bi.get
                    }
                    if (mdl != null)
                        mdl.materialData += str -> bi.get
                } catch {
                    case e: Exception => throw new IOException(str, e)
                }
            }
        }
    }

    // Automatically creates a manifest.
    def createManifestForLocal(name: String): DataManifestCreationResult = {
        val dmcr = new DataManifestCreationResult
        val rootDir = new File(modelRepository + "/" + name)
        val rootLocator = new FilePMXFilenameLocator(modelRepository + name + "/")
        val locator = new IPMXFilenameLocator() {
            override def listFiles(): Seq[String] = rootLocator.listFiles

            override def apply(filename: String): Array[Byte] = {
                val data = rootLocator(filename)
                if (dmcr.filesToHashes.contains(filename)) data
                val hash = hashRawBytes(data)
                dmcr.filesToHashes.put(filename, hash)
                data
            }
        }
        // This way absolutely ensures the model is valid before we upload,
        // and it means we don't have to keep a "DM dry run" copy of the loading logic.
        // Plus, if it occurs *before* the model is loaded, it won't be loaded twice!
        dmcr.animSet = getInternal(locator, name, true).defaultAnims

        return dmcr
    }

    def hashRawBytes(data: Array[Byte]): String = {
        // NOTE: This hash does NOT need to be cryptographically secure, just
        // large enough to avoid any decent chance of accidental collision.
        // Oh, and changing it after release will break everything.
        return Hashing.sha256.hashBytes(data).toString
    }

    // Hashes a DataManifest.
    def hashDataManifest(filesToHashes: Map[String, String]): String = {
        val sortedKeys = filesToHashes.keys.map(s => s.toLowerCase).filterNot(_.endsWith(".txt")).filterNot(_.endsWith(".dat")).toList.sorted
        var valueMap = Map[String, String]()
        filesToHashes.keys.foreach((k) => valueMap += k.toLowerCase -> filesToHashes(k))
        var data = ""
        sortedKeys.foreach(k => {
            data += valueMap(k)
        })
        hashRawBytes(data.getBytes)
    }

    trait IPMXFilenameLocator {
        def apply(filename: String): Array[Byte]

        def listFiles(): Seq[String]
    }

    // Upload requires re-reading the files, and automatically creates a data
    // manifest.
    // It then stores the mappings from hashes to files for when the server asks
    // for them.

    class FilePMXFilenameLocator(val baseDir: String) extends IPMXFilenameLocator {
        def listFiles(base: String): Seq[String] = {
            var strseq = Seq.empty[String]
            val fileList = new File(baseDir + base).listFiles()
            if (fileList != null) {
                fileList.foreach(f => {
                    if (f.isDirectory()) {
                        val prefix = f.getName + "/"
                        strseq = strseq ++ listFiles(base + prefix).map(v => prefix + v)
                    } else {
                        strseq = strseq :+ f.getName
                    }
                })
            }
            return strseq
        }

        override def listFiles(): Seq[String] = listFiles("")
        override def apply(filename: String): Array[Byte] = {
            val fis = new FileInputStream(baseDir + filename)
            val data = new Array[Byte](fis.available)
            fis.read(data)
            fis.close()
            return data
        }
    }

    class DownloadingPMXFilenameLocator(val rootFilenameLocator: IPMXFilenameLocator, val targetName: String, val maxTotalUsage: Long) extends IPMXFilenameLocator {
        var totalUsage = 0

        override def listFiles(): Seq[String] = rootFilenameLocator.listFiles

        override def apply(filename: String): Array[Byte] = {
            val targ = new File(modelRepository + targetName + "/" + filename)
            if (targ.exists()) {
                // It exists already, just assume it's right
                val fis = new FileInputStream(targ)
                val data = new Array[Byte](fis.available)
                fis.read(data)
                fis.close()
                return data
            }
            val b = rootFilenameLocator(filename)
            totalUsage += b.length
            if (maxTotalUsage >= 0 && totalUsage > maxTotalUsage)
                throw new IOException(
                    "Potential Denial Of Service attack via HDD usage, download will not be continued.")
            // one final sanity check (lowercase'd because of potential
            // case madness on Windows, etc.)
            if (!targ.getAbsolutePath().toLowerCase()
                .startsWith(new File(modelRepository).getAbsolutePath().toLowerCase()))
            // terminal abusers are not welcome here
                throw new IOException(
                    "Target path outside model repository, a model is dangerous, offensive filename : "
                        + filename.replace("\u001B", "(REALLY DODGY: ^[)"))
            targ.getParentFile().mkdirs()
            val fos = new FileOutputStream(targ)
            fos.write(b)
            fos.close()
            return b
        }
    }

    trait IPMXLocator {
        // If a model is > 2GB, something is seriously wrong with the model and
        // we should run away first chance we get.
        // Not future planning, but seriously, 2GB is a flipping D.O.S attack by
        // my standards.
        def getData(hash: String): Array[Byte]
    }

    class DataManifestCreationResult {
        var filesToHashes = collection.mutable.Map[String, String]()
        var animSet: AnimSet = _
    }

}
