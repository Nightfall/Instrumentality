# Instrumentality
The Miku playermodel project.

This is made up of:

1. A common library, containing the animation code, PMX loader, PMX renderer for LWJGL, etc.

2. The MC mod for 1.7.10

## Current status

Other Minecraft versions are on the TODO list.

We need to get a few premade AnimSets into the system.

PlayerControlAnimation's MC-specific way of handling things should be moved out somehow.

## Setup

Substitute "idea" for "eclipse" where required:

1. Do a normal `./gradlew build idea` in core, and a `./gradlew setupDecompWorkspace build idea` in mc1710

2. Open the mc1710 project & setup run config, then add Core as a module

3. Remove the JAR dependency in mc1710 and replace it with a project dependency

4. Put your model into the folder "run" in the sub-folder "mdl"
   (you may have to create it) in a sub-sub-folder using the name "mdl.pmx".
   Don't forget to add all textures relative to the model, and note that all filenames are made lowercase
   (because some model authors aren't consistent. This matters on case-sensitive filesystems)

The filestructure should be:

    run-+-mdl-+-someModelName-+-mdl.pmx
           |                      |
           |                      +-someTexture.png
           |
           +-someOtherModel-+-mdl.pmx
                            |
                            +-otherTex.png

4. When using, press the = button to open the Editor,
   and select a model to apply it.

## For Editor Testbench Only

1. Delete Minecraft-related code (moe.nightfall.instrumentality.mc)

2. Fix the buildscript

3. Run `./gradlew idea` or `./gradlew eclipse`, depending on what you use

4. Copy native files into the working directory.
   You can get these from the main ZIP supplied here (2.9.3):
   `http://legacy.lwjgl.org/download.php`

5. Setup a run configuration pointing to the main class (moe.nightfall.instrumentality.Main)

6. See step 3 of the ordinary setup, but note that the mdl folder should go wherever you're executing the testbench
