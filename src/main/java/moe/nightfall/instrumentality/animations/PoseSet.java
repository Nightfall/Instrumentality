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
package moe.nightfall.instrumentality.animations;

import moe.nightfall.instrumentality.PoseBoneTransform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of poses that can be parented to each other & such
 * Created on 11/09/15.
 */
public class PoseSet {
    public HashMap<String, PoseAnimation> allPoses = new HashMap<String, PoseAnimation>();
    public HashMap<String, String> poseParents = new HashMap<String, String>();

    public PoseSet() {
        allPoses.put("idle", new PoseAnimation());
        allPoses.put("lookL", new PoseAnimation());
        poseParents.put("lookL", "idle");
        allPoses.put("lookR", new PoseAnimation());
        poseParents.put("lookR", "idle");
        allPoses.put("lookU", new PoseAnimation());
        poseParents.put("lookU", "idle");
        allPoses.put("lookD", new PoseAnimation());
        poseParents.put("lookD", "idle");
        allPoses.put("holdItem", new PoseAnimation());
        poseParents.put("holdItem", "idle");
        allPoses.put("useItemP25", new PoseAnimation());
        poseParents.put("useItemP25", "holdItem");
        allPoses.put("useItemP50", new PoseAnimation());
        poseParents.put("useItemP50", "useItemP25");
        allPoses.put("useItemP75", new PoseAnimation());
        poseParents.put("useItemP75", "useItemP50");
        allPoses.put("stabItem", new PoseAnimation());
        poseParents.put("stabItem", "holdItem");
        allPoses.put("bowPullSItem", new PoseAnimation());
        poseParents.put("bowPullSItem", "holdItem");
        allPoses.put("bowPullEItem", new PoseAnimation());
        poseParents.put("bowPullEItem", "bowPullSItem");

        allPoses.put("walkStart", new PoseAnimation());
        poseParents.put("walkStart", "idle");

        // when left foot hits ground, R ankle should be lifted but toes not actually off ground at this point
        allPoses.put("walkLFHit", new PoseAnimation());
        poseParents.put("walkLFHit", "idle");

        // Left and right feet are both central from a top-down perspective,
        // from side, right is raised
        allPoses.put("walkLFMidpoint", new PoseAnimation());
        poseParents.put("walkLFMidpoint", "idle");

        // right foot hits ground, (see walkLFHit)
        allPoses.put("walkRFHit", new PoseAnimation());
        poseParents.put("walkRFHit", "idle");

        // Right foot reaches midpoint, see walkLFMidpoint but flip which feet are involved
        allPoses.put("walkRFMidpoint", new PoseAnimation());
        poseParents.put("walkRFMidpoint", "idle");

        allPoses.put("falling", new PoseAnimation());
        poseParents.put("falling", "idle");

        allPoses.put("sneaking", new PoseAnimation());
        poseParents.put("sneaking", "idle");

        allPoses.put("hairL", new PoseAnimation());
        poseParents.put("hairL", "idle");

        allPoses.put("hairR", new PoseAnimation());
        poseParents.put("hairR", "idle");
    }

    public Animation createAnimation(HashMap<String, Double> poseStrengths) {
        OverlayAnimation rootOA = new OverlayAnimation();
        HashMap<String, Double> hashMap = new HashMap<String, Double>();
        for (Map.Entry<String, Double> entries : poseStrengths.entrySet()) {
            String n = entries.getKey();
            double tVal = entries.getValue();
            while (n != null) {
                Double d = hashMap.get(n);
                if (d == null) {
                    hashMap.put(n, tVal);
                } else {
                    if (d < tVal)
                        hashMap.put(n, tVal);
                }
                n = poseParents.get(n);
                if (tVal > 1.0d)
                    tVal = 1.0d;
            }
        }
        for (Map.Entry<String, Double> entries : hashMap.entrySet())
            rootOA.subAnimations.add(new StrengthMultiplyAnimation(allPoses.get(entries.getKey()), (float) ((double) entries.getValue())));
        return rootOA;
    }

    public Animation createEditAnimation(String n) {
        HashMap<String, Double> hashMap = new HashMap<String, Double>();
        while (n != null) {
            hashMap.put(n, 1d);
            n = poseParents.get(n);
        }
        return createAnimation(hashMap);
    }

    public void save(DataOutputStream os) throws IOException {
        os.writeUTF("Lelouch");
        for (Map.Entry<String, PoseAnimation> pose : allPoses.entrySet()) {
            // if we ever have the unfortunate situation of having multiple versions, here's how to check
            // VERSION NAMES: Lelouch Kallen Yukki Yuno
            // (Version names should alternate between genders. We're an equal opportunity name-grabber.)
            os.write(1);
            os.writeUTF(pose.getKey());
            HashMap<String, PoseBoneTransform> pbts = pose.getValue().hashMap;
            for (Map.Entry<String, PoseBoneTransform> pbt : pbts.entrySet()) {
                os.write(1);
                os.writeUTF(pbt.getKey());
                os.writeDouble(pbt.getValue().X0);
                os.writeDouble(pbt.getValue().X1);
                os.writeDouble(pbt.getValue().X2);
                os.writeDouble(pbt.getValue().Y0);
                os.writeDouble(pbt.getValue().Y1);
                os.writeDouble(pbt.getValue().Z0);
                os.writeDouble(pbt.getValue().TX0);
                os.writeDouble(pbt.getValue().TY0);
                os.writeDouble(pbt.getValue().TZ0);
            }
            os.write(0);
        }
        os.write(0);
    }

    public void load(DataInputStream dis) throws IOException {
        String ver = dis.readUTF();
        if (ver.equals("Lelouch")) {
            while (dis.read() != 0) {
                String poseName = dis.readUTF();
                PoseAnimation pa = new PoseAnimation();
                allPoses.put(poseName, pa);
                while (dis.read() != 0) {
                    PoseBoneTransform pbt = new PoseBoneTransform();
                    pa.hashMap.put(dis.readUTF(), pbt);
                    pbt.X0 = dis.readDouble();
                    pbt.X1 = dis.readDouble();
                    pbt.X2 = dis.readDouble();
                    pbt.Y0 = dis.readDouble();
                    pbt.Y1 = dis.readDouble();
                    pbt.Z0 = dis.readDouble();
                    pbt.TX0 = dis.readDouble();
                    pbt.TY0 = dis.readDouble();
                    pbt.TZ0 = dis.readDouble();
                }
            }
        } else {
            throw new IOException("Not posedata!");
        }
    }
}
