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

import java.io.{FileOutputStream, DataOutputStream}

/**
 * Makes editing the URLs in PoseSets easier.
 * Created on 26/09/15.
 */
object URLEditorMain extends App {
    print("Enter modelname with poseset to edit URL data in: ")
    val mdl = ModelCache.getLocal(scala.io.StdIn.readLine())
    print("Enter new URL: ")
    mdl.poses.downloadURL = scala.io.StdIn.readLine()
    print("Enter folder within ZIP, or blank for web-link. Must end with '/' unless blank: ")
    mdl.poses.downloadBaseFolder = scala.io.StdIn.readLine()
    println("Stop the process now to cancel writing to cfgpose.dat in the current directory.")
    println("Otherwise, press enter.")
    scala.io.StdIn.readLine()
    println("Saving...")
    val fos = new FileOutputStream("cfgpose.dat")
    mdl.poses.save(new DataOutputStream(fos))
    fos.close()
    println("Saved.")
}
