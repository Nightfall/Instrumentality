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

/**
 * Created on 19/08/15.
 */
public final class ModelCache {

    // Local model repository
    public static String modelRepository = "mdl/";

    private ModelCache() {

    }

    private static HashMap<String, PMXModel> localModels = new HashMap<String, PMXModel>();

    public static PMXModel getByManifest(final HashMap<String, String> hashMap, final IPMXLocator remoteServer) {
        // Check for eligible candidates locally
        final String targetHash = hashMap.get("mdl.pmx");
        for (String s : getLocalModels()) {
            try {
                IPMXFilenameLocator l = new FilePMXFilenameLocator(modelRepository + "/" + s.toLowerCase() + "/");
                if (targetHash.equalsIgnoreCase(Hashing.sha1().hashBytes(l.getData("mdl.pmx")).toString()))
                    return getLocal(s);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        try {
            return getInternal(new IPMXFilenameLocator() {
                @Override
                public byte[] getData(String filename) throws IOException {
                    byte[] b = remoteServer.getData(hashMap.get(filename.toLowerCase()));
                    File targ = new File(modelRepository + "/" + targetHash.toLowerCase() + "/" + (filename.toLowerCase()));
                    targ.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(targ);
                    fos.write(b);
                    fos.close();
                    return b;
                }
            }, targetHash);
        } catch (IOException ioe) {
            return null;
        }
    }

    public static Iterable<String> getLocalModels() {
        String[] out = new File(modelRepository).list();
        ArrayList<String> als = new ArrayList<String>(out.length);
        for (String s : out)
            als.add(s);
        return als;
    }

    public static PMXModel getLocal(String name) {
        PMXModel mdl = localModels.get(name.toLowerCase());
        if (mdl != null)
            return mdl;
        try {
            mdl = getInternal(new FilePMXFilenameLocator(modelRepository + "/" + name.toLowerCase() + "/"), name);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
        return mdl;
    }

    private static PMXModel getInternal(IPMXFilenameLocator locator, String name) throws IOException {
        PMXModel pm = new PMXModel(new PMXFile(locator.getData("mdl.pmx")), Loader.groupSize);
        loadTextures(pm, locator);
        localModels.put(name.toLowerCase(), pm);
        return pm;
    }

    private static void loadTextures(PMXModel mdl, IPMXFilenameLocator fl) throws IOException {
        for (PMXFile.PMXMaterial mat : mdl.theFile.matData) {
            String str = mat.texTex.toLowerCase();
            // It's dumb, but this is the only place arbitrary pathnames can be entered into that we'll accept.
            // So we have to security-check it. Please, fix this if there is a problem.
            if (str.contains("..") || str.contains(":"))
                throw new IOException("Potentially security-threatening string found");
            if (str == null)
                continue;
            try {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(fl.getData(str)));
                mdl.materialData.put(str, bi);
            } catch (Exception e) {
                throw new IOException(str, e);
            }
        }
    }

    // Automatically creates a manifest, and a way of mapping hashes back to files for use when requests are made
    public static DataManifestCreationResult createManifestForLocal(String name) throws IOException {
        final DataManifestCreationResult dmcr = new DataManifestCreationResult();
        final IPMXFilenameLocator rootLocator = new FilePMXFilenameLocator(modelRepository + "/" + name.toLowerCase() + "/");
        IPMXFilenameLocator locator = new IPMXFilenameLocator() {
            @Override
            public byte[] getData(String filename) throws IOException {
                filename = filename.toLowerCase();
                byte[] data = rootLocator.getData(filename);
                if (dmcr.filesToHashes.containsKey(filename))
                    return data;
                String hash = Hashing.sha1().hashBytes(data).toString();
                dmcr.filesToHashes.put(filename, hash);
                dmcr.hashesToFiles.put(hash, filename);
                return data;
            }
        };
        PMXFile pf = new PMXFile(locator.getData("mdl.pmx"));
        for (PMXFile.PMXMaterial pm : pf.matData)
            if (pm.texTex != null)
                locator.getData(pm.texTex.toLowerCase());
        return dmcr;
    }

    public interface IPMXFilenameLocator {
        // note that "mdl.pmx" is a reserved name for the PMX file
        byte[] getData(String filename) throws IOException;
    }

    // Upload requires re-reading the files, and automatically creates a data manifest.
    // It then stores the mappings from hashes to files for when the server asks for them.

    public static class FilePMXFilenameLocator implements IPMXFilenameLocator {
        public String baseDir;

        public FilePMXFilenameLocator(String bDir) {
            baseDir = bDir;
        }

        @Override
        public byte[] getData(String filename) throws IOException {
            FileInputStream fis = new FileInputStream(baseDir + (filename.toLowerCase()));
            byte[] data = new byte[fis.available()];
            fis.read(data);
            fis.close();
            return data;
        }
    }

    public interface IPMXLocator {
        byte[] getData(String hash) throws IOException;
    }

    public static class DataManifestCreationResult {
        public HashMap<String, String> filesToHashes = new HashMap<String, String>();
        public HashMap<String, String> hashesToFiles = new HashMap<String, String>();
    }
}
