package uk.co.gamemanj.instrumentality.animations;

import uk.co.gamemanj.instrumentality.PoseBoneTransform;

import java.util.HashMap;

/**
 * Created on 28/07/15.
 */
public class PoseAnimation implements IAnimation {
    public HashMap<String, PoseBoneTransform> hashMap = new HashMap<>();

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        return hashMap.get(boneName);
    }

    @Override
    public void update(double deltaTime) {

    }
}
