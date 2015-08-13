package moe.nightfall.instrumentality;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A loaded PMX file.
 * Note that this is separate from a model playing an animation, so to reduce memory usage, create one of these and attach models to it.
 *
 * @author gamemanj
 *         Created on 24/07/15.
 */
public class PMXFile {
    public String localCharname, globalCharname;
    public String localComment, globalComment;
    public PMXVertex[] vertexData;
    public int[][] faceData;
    public PMXMaterial[] matData;
    public PMXBone[] boneData;

    public PMXFile(byte[] pmxFile) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(pmxFile);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        if (bb.get() != (byte) 'P')
            throw new IOException("Not PMX file");
        if (bb.get() != (byte) 'M')
            throw new IOException("Not PMX file");
        if (bb.get() != (byte) 'X')
            throw new IOException("Not PMX file");
        if (bb.get() != (byte) ' ')
            throw new IOException("Not PMX file");
        if (bb.getFloat() != 2.0f)
            throw new IOException("Sorry, only V2.0 PMX files supported");
        if (bb.get() != 8)
            throw new IOException("Data Count should be 8");
        int textEncoding = bb.get();
        if (bb.get() != 0)
            throw new IOException("We don't have any way to handle extended UVs, thus denying them");
        int vertexIS = bb.get();
        int textureIS = bb.get();
        int materialIS = bb.get();
        int boneIS = bb.get();
        int morphIS = bb.get();
        int rigidIS = bb.get();
        localCharname = getText(bb, textEncoding);
        globalCharname = getText(bb, textEncoding);
        localComment = getText(bb, textEncoding);
        globalComment = getText(bb, textEncoding);
        vertexData = new PMXVertex[bb.getInt()];
        for (int i = 0; i < vertexData.length; i++)
            vertexData[i] = readVertex(bb, boneIS);
        int faceVData = bb.getInt();
        if (faceVData % 3 != 0)
            throw new IOException("Invalid facecount, must be multiple of 3");
        faceData = new int[faceVData / 3][3];
        for (int i = 0; i < faceVData; i += 3) {
            faceData[i / 3][0] = getIndex(bb, vertexIS);
            faceData[i / 3][1] = getIndex(bb, vertexIS);
            faceData[i / 3][2] = getIndex(bb, vertexIS);
        }
        String[] texData = new String[bb.getInt()];
        for (int i = 0; i < texData.length; i++)
            texData[i] = getText(bb, textEncoding);
        matData = new PMXMaterial[bb.getInt()];
        for (int i = 0; i < matData.length; i++)
            matData[i] = readMaterial(bb, textEncoding, textureIS, texData);
        boneData = new PMXBone[bb.getInt()];
        for (int i = 0; i < boneData.length; i++)
            boneData[i] = readBone(bb, textEncoding, boneIS);
    }

    private PMXBone readBone(ByteBuffer bb, int textEncoding, int boneIS) throws IOException {
        PMXBone pb = new PMXBone();
        pb.localName = getText(bb, textEncoding);
        pb.globalName = getText(bb, textEncoding);
        System.out.println(pb.globalName);
        pb.posX = -bb.getFloat();
        pb.posY = bb.getFloat();
        pb.posZ = bb.getFloat();
        pb.parentBoneIndex = getIndex(bb, boneIS);
        pb.transformLevel = bb.getInt();
        int i = bb.getShort();
        pb.flagConnection = (i & 0x0001) != 0;
        pb.flagRotatable = (i & 0x0002) != 0;
        pb.flagMovable = (i & 0x0004) != 0;
        pb.flagDisplay = (i & 0x0008) != 0;
        pb.flagCanOperate = (i & 0x0010) != 0;
        pb.flagIK = (i & 0x0020) != 0;
        pb.flagAddLocalDeform = (i & 0x0080) != 0;
        pb.flagAddRotation = (i & 0x0100) != 0;
        pb.flagAddMovement = (i & 0x0200) != 0;
        pb.flagFixedAxis = (i & 0x0400) != 0;
        pb.flagLocalAxis = (i & 0x0800) != 0;
        pb.flagPhysicalTransform = (i & 0x1000) != 0;
        pb.flagExternalParentTransform = (i & 0x2000) != 0;
        if (pb.flagConnection) {
            pb.connectionIndex = getIndex(bb, boneIS);
        } else {
            pb.connectionPosOfsX = -bb.getFloat();
            pb.connectionPosOfsY = bb.getFloat();
            pb.connectionPosOfsZ = bb.getFloat();
        }
        if (pb.flagAddRotation) {
            pb.addRotationParentIndex = getIndex(bb, boneIS);
            pb.addRotationRate = bb.getFloat();
        }
        if (pb.flagFixedAxis) {
            pb.fixedAxisX = -bb.getFloat();
            pb.fixedAxisY = bb.getFloat();
            pb.fixedAxisZ = bb.getFloat();
        }
        if (pb.flagLocalAxis) {
            // TODO: What are these values, and do they need to be X-flipped
            pb.localAxisXX = bb.getFloat();
            pb.localAxisXY = bb.getFloat();
            pb.localAxisXZ = bb.getFloat();
            pb.localAxisZX = bb.getFloat();
            pb.localAxisZY = bb.getFloat();
            pb.localAxisZZ = bb.getFloat();
        }
        if (pb.flagExternalParentTransform)
            pb.externalParentTransformKeyValue = bb.getInt();
        if (pb.flagIK) {
            getIndex(bb, boneIS);
            bb.getInt();
            bb.getFloat();
            int lc = bb.getInt();
            for (int i2 = 0; i2 < lc; i2++) {
                getIndex(bb, boneIS);
                if (bb.get() != 0) {
                    bb.getFloat();
                    bb.getFloat();
                    bb.getFloat();
                    bb.getFloat();
                    bb.getFloat();
                    bb.getFloat();
                }
            }
        }
        return pb;
    }

    private PMXMaterial readMaterial(ByteBuffer bb, int textEncoding, int textureIS, String[] tex) throws IOException {
        PMXMaterial pm = new PMXMaterial();
        pm.localName = getText(bb, textEncoding);
        pm.globalName = getText(bb, textEncoding);
        pm.diffR = bb.getFloat();
        pm.diffG = bb.getFloat();
        pm.diffB = bb.getFloat();
        pm.diffA = bb.getFloat();
        pm.specR = bb.getFloat();
        pm.specG = bb.getFloat();
        pm.specB = bb.getFloat();
        pm.specStrength = bb.getFloat();
        pm.ambR = bb.getFloat();
        pm.ambG = bb.getFloat();
        pm.ambB = bb.getFloat();
        pm.drawMode = bb.get();
        pm.edgeR = bb.getFloat();
        pm.edgeG = bb.getFloat();
        pm.edgeB = bb.getFloat();
        pm.edgeA = bb.getFloat();
        pm.edgeSize = bb.getFloat();
        int ind = getIndex(bb, textureIS);
        pm.texTex = (ind < 0) ? null : tex[ind];
        ind = getIndex(bb, textureIS);
        pm.texEnv = (ind < 0) ? null : tex[ind];
        pm.envMode = bb.get();
        pm.toonFlag = bb.get();
        switch (pm.toonFlag) {
            case 0:
                ind = getIndex(bb, textureIS);
                pm.texToon = (ind < 0) ? null : tex[ind];
                break;
            case 1:
                pm.toonIndex = bb.get();
                break;
            default:
                throw new IOException("Unknown ToonFlag " + pm.toonFlag);
        }
        pm.memo = getText(bb, textEncoding);
        pm.faceCount = bb.getInt();
        if ((pm.faceCount % 3) != 0)
            throw new IOException("Material facecount % 3 != 0");
        pm.faceCount /= 3;
        return pm;
    }

    private PMXVertex readVertex(ByteBuffer bb, int boneIS) throws IOException {
        PMXVertex pmxVertex = new PMXVertex();
        pmxVertex.posX = -bb.getFloat();
        pmxVertex.posY = bb.getFloat();
        pmxVertex.posZ = bb.getFloat();
        pmxVertex.normalX = -bb.getFloat();
        pmxVertex.normalY = bb.getFloat();
        pmxVertex.normalZ = bb.getFloat();
        pmxVertex.texU = bb.getFloat();
        pmxVertex.texV = bb.getFloat();
        pmxVertex.weightType = bb.get();
        switch (pmxVertex.weightType) {
            case 0:
                pmxVertex.boneIndices[0] = getIndex(bb, boneIS);
                break;
            case 1:
                pmxVertex.boneIndices[0] = getIndex(bb, boneIS);
                pmxVertex.boneIndices[1] = getIndex(bb, boneIS);
                pmxVertex.boneWeights[0] = bb.getFloat();
                break;
            case 2:
                pmxVertex.boneIndices[0] = getIndex(bb, boneIS);
                pmxVertex.boneIndices[1] = getIndex(bb, boneIS);
                pmxVertex.boneIndices[2] = getIndex(bb, boneIS);
                pmxVertex.boneIndices[3] = getIndex(bb, boneIS);
                pmxVertex.boneWeights[0] = bb.getFloat();
                pmxVertex.boneWeights[1] = bb.getFloat();
                pmxVertex.boneWeights[2] = bb.getFloat();
                pmxVertex.boneWeights[3] = bb.getFloat();
                break;
            case 3:
                pmxVertex.boneIndices[0] = getIndex(bb, boneIS);
                pmxVertex.boneIndices[1] = getIndex(bb, boneIS);
                pmxVertex.boneWeights[0] = bb.getFloat();
                pmxVertex.sdefCX = -bb.getFloat();
                pmxVertex.sdefCY = bb.getFloat();
                pmxVertex.sdefCZ = bb.getFloat();
                pmxVertex.sdefR0X = -bb.getFloat();
                pmxVertex.sdefR0Y = bb.getFloat();
                pmxVertex.sdefR0Z = bb.getFloat();
                pmxVertex.sdefR1X = -bb.getFloat();
                pmxVertex.sdefR1Y = bb.getFloat();
                pmxVertex.sdefR1Z = bb.getFloat();
                break;
            default:
                throw new IOException("unknown weight def " + pmxVertex.weightType);
        }
        pmxVertex.edgeScale = bb.getFloat();
        return pmxVertex;
    }


    private String getText(ByteBuffer bb, int textEncoding) {
        byte[] data = new byte[bb.getInt()];
        bb.get(data);
        try {
            return new String(data, (textEncoding == 1) ? "UTF-8" : "UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "~ unsupported encoding ~";
        }
    }

    private int getIndex(ByteBuffer bb, int type) throws IOException {
        switch (type) {
            case 1:
                return bb.get();
            case 2:
                return bb.getShort();
            case 4:
                return bb.getInt();
            default:
                throw new IOException("unknown index type " + type);
        }
    }

    public static class PMXVertex {
        public float posX, posY, posZ;
        public float normalX, normalY, normalZ;
        public float texU, texV;
        public float edgeScale;
        public int weightType;
        public int[] boneIndices = new int[4];
        public float[] boneWeights = new float[4];
        public float sdefCX, sdefCY, sdefCZ;
        public float sdefR0X, sdefR0Y, sdefR0Z;
        public float sdefR1X, sdefR1Y, sdefR1Z;
    }

    public static class PMXMaterial {
        public String localName, globalName;
        public float diffR, diffG, diffB, diffA;
        public float specR, specG, specB;
        public float specStrength;
        public float ambR, ambG, ambB;
        public int drawMode;
        public float edgeR, edgeG, edgeB, edgeA;
        public float edgeSize;
        public String texTex, texEnv, texToon; // the texture pool isn't needed
        public int envMode;
        // toonFlag uses toonIndex if 1, texToon if 0
        public int toonFlag, toonIndex;
        public String memo;
        public int faceCount; // Note that this is divided by 3 to match with faceData
    }

    public class PMXBone {
        public String localName, globalName;
        public float posX, posY, posZ;
        public int parentBoneIndex;
        public int transformLevel;
        public boolean flagConnection, flagRotatable, flagMovable, flagDisplay, flagCanOperate, flagIK, flagAddLocalDeform, flagAddRotation, flagAddMovement, flagFixedAxis, flagLocalAxis, flagPhysicalTransform, flagExternalParentTransform;
        // Connection | Set
        public int connectionIndex;
        // Connection | Unset
        public float connectionPosOfsX, connectionPosOfsY, connectionPosOfsZ;
        // Add Rotation | Set
        public int addRotationParentIndex;
        public float addRotationRate;
        // Fixed Axis | Set
        public float fixedAxisX, fixedAxisY, fixedAxisZ;
        // Local Axis | Set
        public float localAxisXX, localAxisXY, localAxisXZ;
        public float localAxisZX, localAxisZY, localAxisZZ;
        // External Parent Transform | Set
        public int externalParentTransformKeyValue;
        // IK Data not included
    }
}
