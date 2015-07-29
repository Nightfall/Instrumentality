package uk.co.gamemanj.instrumentality.animations;

/**
 * Introspectable animation libraries for usage in emote systems & such
 * Created on 28/07/15.
 */
public interface IAnimationLibrary {
    IAnimation getPose(String poseName);

    String[] getPoses();
}
