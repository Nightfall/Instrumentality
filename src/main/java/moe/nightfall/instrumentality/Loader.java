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

import moe.nightfall.instrumentality.animations.IAnimationLibrary;
import moe.nightfall.instrumentality.animations.libraries.EmoteAnimationLibrary;
import moe.nightfall.instrumentality.animations.libraries.PlayerAnimationLibrary;
import moe.nightfall.instrumentality.shader.Shader;
import moe.nightfall.instrumentality.shader.ShaderManager;

import java.util.LinkedList;

public class Loader {

    // TODO Move
    public static final int groupSize = 12;

    // This is the current model name for the player.
    // To be notified when this changes, put yourself on the list below :)
    public static String currentFile = "miku";
    public static LinkedList<Runnable> currentFileListeners = new LinkedList<Runnable>();

    public static Shader shaderBoneTransform;

    public static IAnimationLibrary[] animLibs;
    public static EmoteAnimationLibrary ial_e;
    public static PlayerAnimationLibrary ial_p;

    public static void setup() throws Exception {
        loadModel();
        loadShaders();
    }

    public static void loadShaders() {
        ShaderManager.loadShaders();
    }

    public static void loadModel() throws Exception {
        shaderBoneTransform = ShaderManager.createProgram("/assets/instrumentality/shader/bone_transform.vert",
                "/assets/instrumentality/shader/bone_transform.frag").set("groupSize", groupSize);

        // animation libraries are NOT a per-model thing
        ial_e = new EmoteAnimationLibrary();
        ial_p = new PlayerAnimationLibrary();
        animLibs = new IAnimationLibrary[]{ial_e, ial_p};
    }

}
