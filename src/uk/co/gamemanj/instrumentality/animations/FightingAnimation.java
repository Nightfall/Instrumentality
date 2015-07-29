package uk.co.gamemanj.instrumentality.animations;

import uk.co.gamemanj.instrumentality.PoseBoneTransform;

/**
 * Created on 27/07/15.
 */
public class FightingAnimation implements IAnimation {

    public float aPos;

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        float usage = aPos;
        float block = 0;
        if (usage < 0) {
            block = -usage;
            usage = 0;
        }

        if (boneName.equalsIgnoreCase("L_shouler")) {
            PoseBoneTransform holdingPBT = new PoseBoneTransform();
            PoseBoneTransform usagePBT = new PoseBoneTransform();
            usagePBT.X0 = (float) Math.toRadians(20);
            usagePBT.Y0 = (float) Math.toRadians(30);
            PoseBoneTransform blockPBT = new PoseBoneTransform();
            blockPBT.X0 = (float) Math.toRadians(35.3f);
            blockPBT.Y0 = (float) Math.toRadians(-6f);
            blockPBT.Z0 = (float) Math.toRadians(-1.7f);
            PoseBoneTransform pbt = new PoseBoneTransform(holdingPBT, usagePBT, usage);
            return new PoseBoneTransform(pbt, blockPBT, block);
        }
        if (boneName.equalsIgnoreCase("L_ellbow")) {
            PoseBoneTransform holdingPBT = new PoseBoneTransform();
            PoseBoneTransform usagePBT = new PoseBoneTransform();
            usagePBT.X0 = (float) Math.toRadians(40);
            usagePBT.Z0 = (float) Math.toRadians(80);
            PoseBoneTransform blockPBT = new PoseBoneTransform();
            blockPBT.X0 = (float) Math.toRadians(43.5f);
            blockPBT.Y0 = (float) Math.toRadians(103);
            blockPBT.Z0 = (float) Math.toRadians(20);
            blockPBT.X1 = (float) Math.toRadians(-47.7f);
            PoseBoneTransform pbt = new PoseBoneTransform(holdingPBT, usagePBT, usage);
            return new PoseBoneTransform(pbt, blockPBT, block);
        }
        if (boneName.equalsIgnoreCase("L_hand")) {
            PoseBoneTransform holdingPBT = new PoseBoneTransform();
            PoseBoneTransform usagePBT = new PoseBoneTransform();
            PoseBoneTransform blockPBT = new PoseBoneTransform();
            PoseBoneTransform pbt = new PoseBoneTransform(holdingPBT, usagePBT, usage);
            return new PoseBoneTransform(pbt, blockPBT, block);
        }

        return null;
    }

    @Override
    public void update(double deltaTime) {

    }
}
