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

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * I'd replace half of this with a quaternion if I wasn't afraid it wouldn't interpolate.
 * Created on 24/07/15.
 */
public class PoseBoneTransform {
    /**
     * The rotation values(applied in that order)
     * The reason for having 3 sets of values is to allow some rotations to happen after/before others.
     * (for example: rotation on one axis to raise/lower head should be applied after left/right rotation)
     */
    public float X0, Y0, Z0;
    public float X1, Y1;
    public float X2;

    /**
     * The translation values(applied before rotation)
     */
    public float TX0, TY0, TZ0;

    public PoseBoneTransform() {

    }

    /**
     * My Little Miku
     * Interpolation Is Magic
     *
     * @param A The pose to interpolate from.
     * @param B The pose to interpolate to.
     * @param i The interpolation value.
     */

    public PoseBoneTransform(PoseBoneTransform A, PoseBoneTransform B, float i) {
        i = 1.0f - i;
        if (A == null)
            A = new PoseBoneTransform();
        if (B == null)
            B = new PoseBoneTransform();
        X0 = (A.X0 * i) + (B.X0 * (1.0f - i));
        Y0 = (A.Y0 * i) + (B.Y0 * (1.0f - i));
        Z0 = (A.Z0 * i) + (B.Z0 * (1.0f - i));
        X1 = (A.X1 * i) + (B.X1 * (1.0f - i));
        Y1 = (A.Y1 * i) + (B.Y1 * (1.0f - i));
        X2 = (A.X2 * i) + (B.X2 * (1.0f - i));

        TX0 = (A.TX0 * i) + (B.TX0 * (1.0f - i));
        TY0 = (A.TY0 * i) + (B.TY0 * (1.0f - i));
        TZ0 = (A.TZ0 * i) + (B.TZ0 * (1.0f - i));
    }

    public PoseBoneTransform(PoseBoneTransform boneTransform) {
        X0 = boneTransform.X0;
        Y0 = boneTransform.Y0;
        Z0 = boneTransform.Z0;
        X1 = boneTransform.X1;
        Y1 = boneTransform.Y1;
        X2 = boneTransform.X2;
        TX0 = boneTransform.TX0;
        TY0 = boneTransform.TY0;
        TZ0 = boneTransform.TZ0;
    }

    public PoseBoneTransform(float v, float v1, float v2, float v3, float v4) {
        X0=v;
        Y0=v1;
        Z0=v2;
        X1=v3;
        Y1=v4;
    }

    public void apply(Matrix4f boneMatrix, boolean translate) {
        if (translate)
            boneMatrix.translate(new Vector3f(TX0, TY0, TZ0));

        boneMatrix.rotate(X0, new Vector3f(1, 0, 0));
        boneMatrix.rotate(Y0, new Vector3f(0, 1, 0));
        boneMatrix.rotate(Z0, new Vector3f(0, 0, 1));
        boneMatrix.rotate(X1, new Vector3f(1, 0, 0));
        boneMatrix.rotate(Y1, new Vector3f(0, 1, 0));
        boneMatrix.rotate(X2, new Vector3f(1, 0, 0));
    }
}
