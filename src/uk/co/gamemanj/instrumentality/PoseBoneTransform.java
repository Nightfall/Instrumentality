package uk.co.gamemanj.instrumentality;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created on 24/07/15.
 */
public class PoseBoneTransform {
    /**
     * The rotation values(applied in that order)
     * The reason for having 3 sets of values is to allow some rotations to happen after/before others.
     * (for example: rotation on one axis to raise/lower head should be applied after left/right rotation)
     */
    public float X0,Y0,Z0;
    public float X1,Y1,Z1;
    public float X2,Y2,Z2;

    /**
     * The translation values(applied before rotation)
     */
    public float TX0,TY0,TZ0;
    public void apply(Matrix4f boneMatrix,boolean translate) {
        if (translate)
            boneMatrix.translate(new Vector3f(TX0,TY0,TZ0));

        boneMatrix.rotate(X0,new Vector3f(1,0,0));
        boneMatrix.rotate(Y0,new Vector3f(0,1,0));
        boneMatrix.rotate(Z0,new Vector3f(0,0,1));
        boneMatrix.rotate(X1,new Vector3f(1,0,0));
        boneMatrix.rotate(Y1,new Vector3f(0,1,0));
        boneMatrix.rotate(Z1,new Vector3f(0,0,1));
        boneMatrix.rotate(X2,new Vector3f(1,0,0));
        boneMatrix.rotate(Y2,new Vector3f(0,1,0));
        boneMatrix.rotate(Z2,new Vector3f(0,0,1));
    }
}
