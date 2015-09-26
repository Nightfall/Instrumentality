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
