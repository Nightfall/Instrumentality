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

import java.io.{ByteArrayInputStream, DataInputStream, File, FileInputStream, FileOutputStream, IOException}
import javax.imageio.ImageIO

import com.google.common.hash.Hashing

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
    var maxTotalUsage = 0L

    // Local model repository.
    // This string, and directories directly under it (but not
    // files/subdirectories within those) should NOT be lowercased.
    var modelRepository = "mdl/"

    private val localModels = collection.concurrent.TrieMap[String, PMXModel]()

    /**
     * Gets a PMXModel from a data manifest. Will try local FS, then try server
     *
     * @param hashMap
     * Mapping from filenames to hashes. Only "mdl.pmx" is needed if
     * remoteServer==null. This is so that MMC-Chat protocol can work
     * @param remoteServer
     * The remote server should a local copy be unavailable (can be
     * null)
     * @return The resulting model
     */
    def getByManifest(hashMap: Map[String, String], remoteServer: IPMXLocator): PMXModel = {
        // Check for eligible candidates locally
        val targetHash = hashMap.get("mdl.pmx").get.toLowerCase()
        getLocalModels() foreach { s =>
            val l = new FilePMXFilenameLocator(modelRepository + "/" + s + "/")
            if (targetHash.equalsIgnoreCase(hashBytes(l("mdl.pmx"))))
                return getLocal(s)
        }
        if (remoteServer == null)
            return null

        try {
            val manifestGetter = new IPMXFilenameLocator() {
                var totalUsage = 0

                override def apply(filename: String): Array[Byte] = {
                    val hash = hashMap.get(filename) getOrElse null
                    if (hash == null)
                        throw new IOException("No file " + filename)
                    val b = remoteServer.getData(hash)
                    totalUsage += b.length
                    if (maxTotalUsage >= 0 && totalUsage > maxTotalUsage)
                        throw new IOException(
                            "Potential Denial Of Service attack via HDD usage, download will not be continued.")
                    val targ = new File(modelRepository + "/" + targetHash + "/" + filename)
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

            // get all .txt files, as they are harmless and probably needed to
            // avoid legal issues
            // Note that the DM creator will deliberately include .txt files for
            // this same purpose
            for ((k, v) <- hashMap) {
                if (k.toLowerCase().endsWith(".txt"))
                    manifestGetter(v)
            }
            manifestGetter("mmcposes.dat")

            return getInternal(manifestGetter, targetHash);
        } catch {
            case e: IOException => return null
        }
    }

    def getLocalModels(): Seq[String] = {
        val out = new File(modelRepository).listFiles()
        return out.filter(_.isDirectory()).map(_.getName)
    }

    def getLocal(name: String): PMXModel = {
        val mdl = localModels.get(name)
        if (mdl.isDefined) return mdl.get
        try {
            return getInternal(new FilePMXFilenameLocator(modelRepository + "/" + name + "/"), name)
        } catch {
            case e: IOException => return null
        }
    }

    private def getInternal(locator: IPMXFilenameLocator, name: String): PMXModel = {
        val pm = new PMXModel(new PMXFile(locator("mdl.pmx")), Loader.groupSize)
        try {
            pm.poses.load(new DataInputStream(new ByteArrayInputStream(locator("mmcposes.dat"))))
        } catch {
            case _: IOException =>
        }

        loadTextures(pm, pm.theFile, locator)
        localModels.put(name, pm)
        return pm
    }

    private def loadTextures(mdl: PMXModel, pf: PMXFile, fl: IPMXFilenameLocator) {
        pf.matData foreach { mat =>
            var str = mat.texTex
            if (str != null) {
                str = str.toLowerCase()
                // It's dumb, but this is the only place arbitrary pathnames can be
                // entered into that we'll accept.
                // So we MUST security-check it. Please, fix this if there is a
                // problem.

                // two dirseps after each other : SUSPICIOUS!
                if (str.matches("[\\\\/][\\\\/]"))
                    throw new IOException("Potentially security-threatening string found (attempt to break into root)");

                // a dirsep at the start of the string: silently remove it
                if (str.matches("^[\\\\/]"))
                    str = str.substring(1);

                // .. : really suspicious!
                if (str.matches("\\.\\."))
                    throw new IOException("Potentially security-threatening string found (attempt to get parent directory)");

                // ./ : just plain weird
                if (str.matches("^\\.[\\\\/]"))
                    str = str.substring(2);

                // /./ : wtf
                if (str.matches("[\\\\/]\\.[\\\\/]"))
                    throw new IOException("Potentially security-threatening string found (weirdness)");

                try {
                    val bi = ImageIO.read(new ByteArrayInputStream(fl(str)))
                    if (mdl != null)
                        mdl.materialData += str -> bi
                } catch {
                    case e: Exception => throw new IOException(str, e)
                }
            }
        }
    }

    // Automatically creates a manifest, and a way of mapping hashes back to
    // files for use when requests are made
    def createManifestForLocal(name: String): DataManifestCreationResult = {
        val dmcr = new DataManifestCreationResult
        val rootDir = new File(modelRepository + "/" + name)
        val rootLocator = new FilePMXFilenameLocator(modelRepository + "/" + name + "/")
        val locator: IPMXFilenameLocator = { filename =>
            val data = rootLocator(filename)
            if (dmcr.filesToHashes.contains(filename)) data
            val hash = hashBytes(data)
            dmcr.filesToHashes.put(filename, hash)
            dmcr.hashesToFiles.put(hash, filename)
            data
        }
        // If we already have this model in RAM, we can skip loading the PMX
        // file itself.
        val alreadyLoaded = localModels.get(name)
        var pf: PMXFile = null
        if (alreadyLoaded.isDefined) {
            pf = alreadyLoaded.get.theFile
            locator("mdl.pmx"); // Needed to ensure it shows up in the
            // manifest
        } else {
            pf = new PMXFile(locator("mdl.pmx"))
        }
        locator("mmcposes.dat")

        // load the textures (Sure, this probably won't be useful for much...
        // except it'll ensure that the uploaded textures are actually valid.)
        loadTextures(null, pf, locator)

        // txt files are also saved (licencing)
        rootDir.listFiles foreach { f =>
            if (f.getName().toLowerCase().endsWith(".txt") && f.isFile())
                locator(f.getName())
        }
        return dmcr;
    }

    private def hashBytes(data: Array[Byte]): String = {
        // NOTE: This hash does NOT need to be cryptographically secure, just
        // large enough to avoid any decent chance of accidental collision.
        // Oh, and changing it after release will break everything.
        return Hashing.sha1.hashBytes(data).toString.substring(0, 24)
    }

    type IPMXFilenameLocator = String => Array[Byte]

    // Upload requires re-reading the files, and automatically creates a data
    // manifest.
    // It then stores the mappings from hashes to files for when the server asks
    // for them.

    class FilePMXFilenameLocator(val baseDir: String) extends IPMXFilenameLocator {

        override def apply(filename: String): Array[Byte] = {
            val fis = new FileInputStream(baseDir + filename)
            val data = new Array[Byte](fis.available)
            fis.read(data)
            fis.close()
            return data
        }
    }

    trait IPMXLocator {
        // If a model is > 2GB, something is seriously wrong with the model and
        // we should run away first chance we get.
        // Not future planning, but seriously, 2GB is a flipping D.O.S attack by
        // my standards.
        def getLength(hash: String): Int

        def getData(hash: String): Array[Byte]
    }

    class DataManifestCreationResult {
        var filesToHashes = collection.mutable.Map[String, String]()
        var hashesToFiles = collection.mutable.Map[String, String]()
    }

}
