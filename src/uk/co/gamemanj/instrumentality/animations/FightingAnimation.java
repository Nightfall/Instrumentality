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
        if (boneName.equalsIgnoreCase("R_shouler")) {
            PoseBoneTransform pbt = new PoseBoneTransform();
            pbt.X0 = (float) Math.toRadians(20);
            pbt.Y0 = (float) Math.toRadians(-30);
            return pbt;
        }
        if (boneName.equalsIgnoreCase("L_shouler")) {
            PoseBoneTransform holdingPBT = new PoseBoneTransform();
            holdingPBT.X0 = (float) Math.toRadians(30);
            holdingPBT.Y0 = (float) Math.toRadians(20);
            PoseBoneTransform usagePBT = new PoseBoneTransform();
            PoseBoneTransform blockPBT = new PoseBoneTransform();
            PoseBoneTransform pbt = new PoseBoneTransform(holdingPBT, usagePBT, usage);
            return new PoseBoneTransform(pbt, blockPBT, block);
        }
        if (boneName.equalsIgnoreCase("L_ellbow")) {
            PoseBoneTransform holdingPBT = new PoseBoneTransform();
            holdingPBT.X0 = (float) Math.toRadians(30);
            PoseBoneTransform usagePBT = new PoseBoneTransform();
            PoseBoneTransform blockPBT = new PoseBoneTransform();
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
