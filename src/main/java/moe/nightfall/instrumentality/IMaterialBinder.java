package moe.nightfall.instrumentality;

/**
 * Created on 24/07/15.
 */
public interface IMaterialBinder {
    /**
     * Binds all the non-trivial (shader & texture, but not colour) attributes of a material
     *
     * @param texture
     */
    void bindMaterial(PMXFile.PMXMaterial texture);
}
