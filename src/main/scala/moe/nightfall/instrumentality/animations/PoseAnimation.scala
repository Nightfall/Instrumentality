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
package moe.nightfall.instrumentality.animations

import moe.nightfall.instrumentality.PoseBoneTransform

import scala.collection.mutable.HashMap

/**
 * Created on 28/07/15.
 */
class PoseAnimation extends Animation {
    val hashMap = new HashMap[String, PoseBoneTransform]

    def this(from: PoseAnimation) {
        this()
        from.hashMap.toSeq.foreach { case (key, value) =>
            hashMap(key) = new PoseBoneTransform(value)
        }
    }

    def this(a: PoseAnimation, b: PoseAnimation, i: Float) {
        this()
        // Handle all cases where A contains the key
        a.hashMap.toSeq.foreach { case (key, value) =>
            hashMap(key) = new PoseBoneTransform(value, if (b.hashMap.contains(key)) b.hashMap(key) else null, i)
        }
        // Handle the remaining cases where only B contains the key
        b.hashMap.toSeq.filter((t: (String, PoseBoneTransform)) => !a.hashMap.contains(t._1)).foreach { case (key, value) =>
            hashMap(key) = new PoseBoneTransform(null, value, i)
        }
    }

    override def getBoneTransform(boneName: String) = hashMap get boneName.toLowerCase
}
