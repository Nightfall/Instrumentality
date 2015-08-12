package moe.nightfall.instrumentality.animations;

import moe.nightfall.instrumentality.PoseBoneTransform;

import java.util.HashMap;

/**
 * Created on 28/07/15.
 */
public class PoseAnimation implements IAnimation {
    public HashMap<String, PoseBoneTransform> hashMap = new HashMap<String, PoseBoneTransform>();

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        return hashMap.get(boneName.toLowerCase());
    }

    @Override
    public void update(double deltaTime) {

    }
}
