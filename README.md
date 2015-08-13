# Instrumentality
H.I.P (aka HMiku playermodel / animation workbench module) is a part of the HMiku playermodel project.

Basically it's a testbench for the animations - you can load PMX models that don't contain SDEFs in it.

The intention is that this will be split into 3 repositories,
or 3 modules in one repository, after work on rendering by Vic is complete.

1. A common library, containing the animation code, PMX loader, PMX renderer for LWJGL, etc.

2. This testbench application(Main.java, basically - it's not a big testbench)

3. The MC mod this will be used for in future (not at all built yet AFAIK -- gamemanj)

## Setup

1. Run `./gradlew idea` or `./gradlew eclipse`, depending on what you use

2. Copy native files into the working directory.
   You can get these from the main ZIP supplied here (2.9.3):
   `http://legacy.lwjgl.org/download.php`

3. Setup a run configuration pointing to the main class (moe.nightfall.instrumentality.Main)

4. Run :)

## LICENSE
TODO
