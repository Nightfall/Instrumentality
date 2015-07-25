package uk.co.gamemanj.instrumentality;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Created on 24/07/15.
 */
public interface IAnimation {
    /**
     * Gets the current transform for a bone.
     * @param boneName The name of a bone.
     * @return Null for no transform(optimization), or the transform to apply to the bone.
     */
    PoseBoneTransform getBoneTransform(String boneName);

    /**
     * Update the animation.
     * @param deltaTime The amount to update by.
     */
    void update(double deltaTime);
}
