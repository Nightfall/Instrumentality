package uk.co.gamemanj.instrumentality.animations;

import org.lwjgl.util.vector.Matrix4f;
import uk.co.gamemanj.instrumentality.PoseBoneTransform;

/**
 * Created on 24/07/15.
 */
public interface IAnimation {
    /**
     * Gets the current transform for a bone.
     * Note that this can be called from any thread, so you shouldn't change state based upon it.
     * The idea is that this is simply a getter.
     * @param boneName The name of a bone.
     * @return Null for no transform(optimization), or the transform to apply to the bone.
     */
    PoseBoneTransform getBoneTransform(String boneName);

    /**
     * Update the animation. Will be called from the "main" thread.
     * (Call them on client tick, no need to call more often, though it's ok if you do)
     * @param deltaTime The amount to update by.
     */
    void update(double deltaTime);
}
