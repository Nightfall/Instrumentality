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

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created on 19/08/15.
 */
public final class ModelCache {

    private ModelCache() {

    }

    private static HashMap<String, PMXModel> localModels = new HashMap<String, PMXModel>();

    public static PMXModel getLocal(String baseDir) throws IOException {
        PMXModel mdl = localModels.get(baseDir);
        if (mdl != null)
            return mdl;
        mdl = getInternal(new FilePMXFilenameLocator(baseDir));
        localModels.put(baseDir, mdl);
        return mdl;
    }

    private static PMXModel getInternal(IPMXFilenameLocator locator) throws IOException {
        PMXModel pm = new PMXModel(new PMXFile(locator.getData("mdl.pmx")), 12);
        loadTextures(pm, locator);
        return pm;
    }

    private static void loadTextures(PMXModel mdl, IPMXFilenameLocator fl) throws IOException {
        for (PMXFile.PMXMaterial mat : mdl.theFile.matData) {
            int bTex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, bTex);

            String str = mat.texTex.toLowerCase();
            if (str == null)
                continue;
            try {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(fl.getData(str)));
                int[] ib = new int[bi.getWidth() * bi.getHeight()];
                bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), ib, 0, bi.getWidth());
                ByteBuffer inb = BufferUtils.createByteBuffer(bi.getWidth() * bi.getHeight() * 4);
                for (int i = 0; i < (bi.getWidth() * bi.getHeight()); i++) {
                    int c = ib[i];
                    inb.put((byte) ((c & 0xFF0000) >> 16));
                    inb.put((byte) ((c & 0xFF00) >> 8));
                    inb.put((byte) (c & 0xFF));
                    inb.put((byte) ((c & 0xFF000000) >> 24));
                }
                inb.rewind();
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, bi.getWidth(), bi.getHeight(), 0, GL11.GL_RGBA,
                        GL11.GL_UNSIGNED_BYTE, inb);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            } catch (Exception e) {
                throw new IOException(str, e);
            }
            mdl.materials.put(str, bTex);
        }
    }

    // Automatically creates a manifest, and a way of mapping hashes back to files for use when requests are made
    public static DataManifestCreationResult createManifestForLocal(String baseDir) throws IOException {
        final DataManifestCreationResult dmcr = new DataManifestCreationResult();
        final IPMXFilenameLocator rootLocator = new FilePMXFilenameLocator(baseDir);
        IPMXFilenameLocator locator = new IPMXFilenameLocator() {
            @Override
            public byte[] getData(String filename) throws IOException {
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
            FileInputStream fis = new FileInputStream(baseDir + filename);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            fis.close();
            return data;
        }
    }

    public interface IPMXLocator {
        byte[] getData(String hash) throws IOException;
    }

    public static class ListPMXLocator implements IPMXLocator {
        private Iterable<IPMXLocator> back;

        public ListPMXLocator(Iterable<IPMXLocator> backend) {
            back = backend;
        }

        @Override
        public byte[] getData(String hash) throws IOException {
            for (IPMXLocator ipl : back) {
                try {
                    byte[] data = ipl.getData(hash);
                    return data;
                } catch (IOException ioe) {

                }
            }
            throw new IOException("Couldn't find data in any locator");
        }
    }

    public static class FilePMXLocator implements IPMXLocator {
        public String baseDir = null;

        public FilePMXLocator(String baseDir) {
            this.baseDir = baseDir;
        }

        public byte[] getData(String hash) throws IOException {
            FileInputStream fis = new FileInputStream(baseDir + hash);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            fis.close();
            return data;
        }
    }

    public static class DataManifestCreationResult {
        public HashMap<String, String> filesToHashes = new HashMap<String, String>();
        public HashMap<String, String> hashesToFiles = new HashMap<String, String>();
    }
}
