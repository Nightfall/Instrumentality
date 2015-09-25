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
package moe.nightfall.instrumentality

import org.lwjgl.util.vector.{Matrix4f, Vector3f}

/**
 * I'd replace half of this with a quaternion if I wasn't afraid it wouldn't interpolate.
 * Created on 24/07/15.
 */
class PoseBoneTransform {

    // TODO SCALA make this immutable / more OPS!

    /**
     * The rotation values(applied in that order)
     * The reason for having 3 sets of values is to allow some rotations to happen after/before others.
     * (for example: rotation on one axis to raise/lower head should be applied after left/right rotation)
     */
    var X0, Y0, Z0: Double = _
    var X1, Y1: Double = _
    var X2: Double = _

    /**
     * The translation values(applied before rotation)
     */
    var TX0, TY0, TZ0: Double = _

    /**
     * My Little Miku
     * Interpolation Is Magic
     *
     * @param A The pose to interpolate from.
     * @param B The pose to interpolate to.
     * @param i The interpolation value.
     */

    def this(A0: PoseBoneTransform, B0: PoseBoneTransform, i0: Float) {
        this()
        val i = 1.0f - i0
        val A = if (A0 == null) new PoseBoneTransform() else A0
        val B = if (B0 == null) new PoseBoneTransform() else B0

        X0 = (A.X0 * i) + (B.X0 * (1.0f - i))
        Y0 = (A.Y0 * i) + (B.Y0 * (1.0f - i))
        Z0 = (A.Z0 * i) + (B.Z0 * (1.0f - i))
        X1 = (A.X1 * i) + (B.X1 * (1.0f - i))
        Y1 = (A.Y1 * i) + (B.Y1 * (1.0f - i))
        X2 = (A.X2 * i) + (B.X2 * (1.0f - i))

        TX0 = (A.TX0 * i) + (B.TX0 * (1.0f - i))
        TY0 = (A.TY0 * i) + (B.TY0 * (1.0f - i))
        TZ0 = (A.TZ0 * i) + (B.TZ0 * (1.0f - i))
    }

    def this(boneTransform: PoseBoneTransform) {
        this()
        X0 = boneTransform.X0
        Y0 = boneTransform.Y0
        Z0 = boneTransform.Z0
        X1 = boneTransform.X1
        Y1 = boneTransform.Y1
        X2 = boneTransform.X2
        TX0 = boneTransform.TX0
        TY0 = boneTransform.TY0
        TZ0 = boneTransform.TZ0
    }

    def this(v: Float, v1: Float, v2: Float, v3: Float, v4: Float) {
        this()
        X0 = v;
        Y0 = v1;
        Z0 = v2;
        X1 = v3;
        Y1 = v4;
    }

    def +=(other: PoseBoneTransform): PoseBoneTransform = {
        val nt = new PoseBoneTransform
        X0 += other.X0
        Y0 += other.Y0
        Z0 += other.Z0
        X1 += other.X1
        Y1 += other.Y1
        X2 += other.X2
        TX0 += other.TX0
        TY0 += other.TY0
        TZ0 += other.TZ0
        return this
    }

    def *=(other: Double): PoseBoneTransform = {
        val nt = new PoseBoneTransform
        X0 *= other
        Y0 *= other
        Z0 *= other
        X1 *= other
        Y1 *= other
        X2 *= other
        TX0 *= other
        TY0 *= other
        TZ0 *= other
        return this
    }

    def isZero(): Boolean = {
        if (X0 != 0)
            return false
        if (X0 != 0)
            return false
        if (Z0 != 0)
            return false
        if (X1 != 0)
            return false
        if (Y1 != 0)
            return false
        if (X2 != 0)
            return false
        if (TX0 != 0)
            return false
        if (TY0 != 0)
            return false
        if (TZ0 != 0)
            return false
        return true
    }

    def isNotZero(): Boolean = !isZero()

    def apply(boneMatrix: Matrix4f) {
        boneMatrix.translate(new Vector3f(TX0.toFloat, TY0.toFloat, TZ0.toFloat));

        boneMatrix.rotate(X0.toFloat, new Vector3f(1, 0, 0))
        boneMatrix.rotate(Y0.toFloat, new Vector3f(0, 1, 0))
        boneMatrix.rotate(Z0.toFloat, new Vector3f(0, 0, 1))
        boneMatrix.rotate(X1.toFloat, new Vector3f(1, 0, 0))
        boneMatrix.rotate(Y1.toFloat, new Vector3f(0, 1, 0))
        boneMatrix.rotate(X2.toFloat, new Vector3f(1, 0, 0))
    }
}
