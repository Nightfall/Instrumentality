# Instrumentality
H.I.P (aka Miku playermodel / animation workbench module) is a part of the Miku playermodel project.

The intention is that this will be split into 3 repositories,
or 3 modules in one repository, after work on rendering by Vic is complete.

1. A common library, containing the animation code, PMX loader, PMX renderer for LWJGL, etc.

2. This testbench application(Main.java, basically - it's not a big testbench)

3. The MC mod this will be used for in future (not at all built yet AFAIK -- gamemanj)

## Current status

We need to separate the majority of the code from the "mc" package as a different repo
The idea is that the MC mod just integrates the code with MC.
Also, PlayerControlAnimation's MC-specific way of handling things should be moved into PlayerModel

## Setup

1. Run `./gradlew setupDecompWorkspace idea` or `./gradlew setupDecompWorkspace eclipse`, depending on what you use

2. Setup a run config as for a normal MC mod

3. Put your model into the folder "eclipse" in the sub-folder "mdl"(you may have to create it) in the sub-sub-folder "miku" using the name "mdl.pmx".
   Don't forget to add all textures relative to the model, and note that all filenames are made lowercase
   (because some model authors aren't consistent. This matters on case-sensitive filesystems)

The filestructure should be:

    eclipse-+-mdl-+-someModelName-+-mdl.pmx
                  |               |
                  |               +-someTexture.png
                  |
                  +-someOtherModel-+-mdl.pmx
                                   |
                                   +-otherTex.png

## For Testbench Only

1. Delete Minecraft-related code (moe.nightfall.instrumentality.mc)

2. Fix the buildscript

3. Run `./gradlew idea` or `./gradlew eclipse`, depending on what you use

4. Copy native files into the working directory.
   You can get these from the main ZIP supplied here (2.9.3):
   `http://legacy.lwjgl.org/download.php`

5. Setup a run configuration pointing to the main class (moe.nightfall.instrumentality.Main)

6. See step 3 of the ordinary setup, but note that the mdl folder should go wherever you're executing the testbench
