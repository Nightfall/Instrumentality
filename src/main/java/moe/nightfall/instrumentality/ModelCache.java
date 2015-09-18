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
package moe.nightfall.instrumentality;

import com.google.common.hash.Hashing;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model cache. This is designed to be used from multiple threads,
 * as long as you do all drawing on the same thread (drawing triggers VBO building and other operations that can't be multithreaded)
 * Created on 19/08/15.
 */
public final class ModelCache {
    // Maximum total usage. Setting this to 0 effectively disables remote server downloading.
    // Setting this to -1 means "unlimited" (NOT RECOMMENDED : this makes it a lot easier for a random player to spam your bandwidth away)
    public static long maxTotalUsage = 0;

    // Local model repository.
    // This string, and directories directly under it (but not files/subdirectories within those) should NOT be lowercased.
    public static String modelRepository = "mdl/";

    private ModelCache() {

    }

    private static ConcurrentHashMap<String, PMXModel> localModels = new ConcurrentHashMap<String, PMXModel>();

    /**
     * Gets a PMXModel from a data manifest. Will try local FS, then try server
     *
     * @param hashMap      Mapping from filenames to hashes. Only "mdl.pmx" is needed if remoteServer==null.
     *                     This is so that MMC-Chat protocol can work
     * @param remoteServer The remote server should a local copy be unavailable (can be null)
     * @return The resulting model
     */
    public static PMXModel getByManifest(final HashMap<String, String> hashMap, final IPMXLocator remoteServer) {
        // Check for eligible candidates locally
        final String targetHash = hashMap.get("mdl.pmx").toLowerCase();
        for (String s : getLocalModels()) {
            try {
                IPMXFilenameLocator l = new FilePMXFilenameLocator(modelRepository + "/" + s + "/");
                if (targetHash.equalsIgnoreCase(hashBytes(l.getData("mdl.pmx"))))
                    return getLocal(s);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        if (remoteServer == null)
            return null;
        try {
            final IPMXFilenameLocator manifestGetter = new IPMXFilenameLocator() {
                @Override
                public Iterator<String> iterator() {
                    return hashMap.keySet().iterator();
                }

                long totalUsage = 0;

                @Override
                public byte[] getData(String filename) throws IOException {
                    String hash = hashMap.get(filename);
                    if (hash == null)
                        throw new IOException("No file " + filename);
                    byte[] b = remoteServer.getData(filename, hash);
                    totalUsage += b.length;
                    if (maxTotalUsage >= 0)
                        if (totalUsage > maxTotalUsage)
                            throw new IOException("Potential Denial Of Service attack via HDD usage, download will not be continued.");
                    File targ = new File(modelRepository + "/" + targetHash + "/" + filename);
                    // one final sanity check (lowercase'd because of potential case madness on Windows, etc.)
                    if (!targ.getAbsolutePath().toLowerCase().startsWith(new File(modelRepository).getAbsolutePath().toLowerCase()))
                        // terminal abusers are not welcome here
                        throw new IOException("Target path outside model repository, a model is dangerous, offensive filename : " + filename.replace("\27", "(REALLY DODGY: ^[)"));
                    targ.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(targ);
                    fos.write(b);
                    fos.close();
                    return b;
                }
            };
            return getInternal(manifestGetter, targetHash, true);
        } catch (IOException ioe) {
            return null;
        }
    }

    public static Iterable<String> getLocalModels() {
        File[] out = new File(modelRepository).listFiles();
        ArrayList<String> als = new ArrayList<String>(out.length);
        for (File s : out) {
            if (s.isDirectory())
                als.add(s.getName());
        }
        return als;
    }

    public static PMXModel getLocal(String name) {
        PMXModel mdl = localModels.get(name);
        if (mdl != null)
            return mdl;
        try {
            mdl = getInternal(new FilePMXFilenameLocator(modelRepository + "/" + name + "/"), name, false);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
        return mdl;
    }

    private static PMXModel getInternal(IPMXFilenameLocator locator, String name, boolean getTxt) throws IOException {
        String pmxFile = "mdl.pmx";
        for (String e : locator) {
            if (e.toLowerCase().endsWith(".pmx"))
                pmxFile = e;
            if (getTxt)
                if (e.toLowerCase().endsWith(".txt"))
                    locator.getData(e);
        }
        PMXModel pm = new PMXModel(new PMXFile(locator.getData(pmxFile)), Loader.groupSize);
        try {
            pm.poses.load(new DataInputStream(new ByteArrayInputStream(locator.getData("mmcposes.dat"))));
        } catch (Exception e) {
            // oh well
        }
        loadTextures(pm, pm.theFile, locator);
        if (!localModels.containsKey(name))
            localModels.put(name, pm);
        return pm;
    }

    private static void loadTextures(PMXModel mdl, PMXFile pf, IPMXFilenameLocator fl) throws IOException {
        for (PMXFile.PMXMaterial mat : pf.matData) {
            String str = mat.texTex;
            if (str == null)
                continue;
            str = str.toLowerCase();
            // It's dumb, but this is the only place arbitrary pathnames can be entered into that we'll accept.
            // So we MUST security-check it. Please, fix this if there is a problem.

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
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(fl.getData(str)));
                if (mdl != null)
                    mdl.materialData.put(str, bi);
            } catch (Exception e) {
                throw new IOException(str, e);
            }
        }
    }

    // Automatically creates a manifest, and a way of mapping hashes back to files for use when requests are made
    public static DataManifestCreationResult createManifestForLocal(String name) throws IOException {
        final DataManifestCreationResult dmcr = new DataManifestCreationResult();
        final IPMXFilenameLocator rootLocator = new FilePMXFilenameLocator(modelRepository + "/" + name + "/");
        IPMXFilenameLocator locator = new IPMXFilenameLocator() {
            @Override
            public Iterator<String> iterator() {
                return dmcr.filesToHashes.keySet().iterator();
            }

            @Override
            public byte[] getData(String filename) throws IOException {
                byte[] data = rootLocator.getData(filename);
                if (dmcr.filesToHashes.containsKey(filename))
                    return data;
                String hash = hashBytes(data);
                dmcr.filesToHashes.put(filename, hash);
                dmcr.hashesToFiles.put(hash, filename);
                return data;
            }
        };
        getInternal(locator, name, true);
        return dmcr;
    }

    private static String hashBytes(byte[] data) {
        // NOTE: This hash does NOT need to be cryptographically secure, just large enough to avoid any decent chance of accidental collision.
        // Oh, and changing it after release will break everything.
        return Hashing.sha1().hashBytes(data).toString().substring(0, 24);
    }

    // A server which holds filenames and
    public interface IPMXFilenameLocator extends Iterable<String> {
        // note that "mdl.pmx" is a reserved name for the PMX file
        // also note that this must NOT return null.
        // that's why we throw IOException :)
        byte[] getData(String filename) throws IOException;
    }

    public static class FilePMXFilenameLocator implements IPMXFilenameLocator {
        public String baseDir;

        public FilePMXFilenameLocator(String bDir) {
            baseDir = bDir;
        }

        @Override
        public byte[] getData(String filename) throws IOException {
            FileInputStream fis = new FileInputStream(baseDir + filename);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            fis.close();
            return data;
        }

        @Override
        public Iterator<String> iterator() {
            // do a recursive find of all files
            LinkedList<String> l = new LinkedList<String>();
            searchDir("", l);
            return l.iterator();
        }

        private void searchDir(String s, LinkedList<String> l) {
            File f = new File(baseDir + s);
            if (f.isDirectory()) {
                for (File cf : f.listFiles())
                    searchDir(s + "/" + cf.getName(), l);
            } else {
                l.add(s);
            }
        }
    }

    // An interface for a server which can get arbitrary data by it's hash, and get the data's length.
    // (IPFS would be a good candidate for this, or an HTTP server.
    //  For HTTP, the filename field is provided.)
    public interface IPMXLocator {
        // If a single file of a model is > 2GB, something is seriously wrong with the model and we should run away first chance we get.
        // Not future planning, but seriously, 2GB is a flipping D.O.S attack by my standards.
        int getLength(String filename, String hash) throws IOException;

        byte[] getData(String filename, String hash) throws IOException;
    }

    public static class DataManifestCreationResult {
        public HashMap<String, String> filesToHashes = new HashMap<String, String>();
        public HashMap<String, String> hashesToFiles = new HashMap<String, String>();
    }
}
