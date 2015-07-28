package uk.co.gamemanj.instrumentality.animations;

import uk.co.gamemanj.instrumentality.PoseBoneTransform;

/**
 * Another reason I like programmatically modifying bone positions!
 * Note that to avoid complications involving stacking animations, this won't attempt to twist the legs/lower body area.
 * Also, you may want to change the way the target speed is calculated.
 *
 * Right now it's based on sneakState, but you may want it to be calculated based on horizontal velocity.
 * Also: Read the variable comments below, you "control" the system via these variables
 *
 * Created on 27/07/15.
 */
public class PlayerControlAnimation implements IAnimation {
    /**
     * 1.0f and -1.0f are directly back-left, back-right. 0.0f is looking directly ahead.
     */
    public float lookLR=1.0f;
    /**
     * 1.0f and -1.0f are directly up/down. 0.0f is looking directly ahead.
     */
    public float lookUD=0.1f;

    /**
     * Set to 1.0f for sneaking,-1.0f for sprinting
     */
    public float sneakStateTarget=0;
    public float sneakState=0;

    /**
     * Set to 1.0f for fighting/placing/using,-1.0f for blocking.
     * Note that fightingStateTarget is automatically set to 0.0f after fightingState(fightingStrengthControl) reaches 1.0f.
     * This allows you to simply set fightingStateTarget to 1.0f every time an "action" happens-
     * the rest is automated by the animation system.
     */
    public float fightingStateTarget = 0;

    /**
     * Used for transitioning to/from the walking animation.
     */
    public boolean walkingFlag=false;

    /**
     * Control of the Walking Animation.
     */
    public WalkingAnimation walking;

    /**
     * Controls the strength of the walking animation.
     * Used to fade in/out the walking animation depending on if the player is, you know, walking.
     */
    public StrengthMultiplyAnimation walkingStrengthControl;

    public FightingAnimation fighting;

    public PlayerControlAnimation(WalkingAnimation wa, StrengthMultiplyAnimation wastr, FightingAnimation fa) {
        walking=wa;
        walkingStrengthControl = wastr;
        fighting = fa;
    }

    @Override
    public PoseBoneTransform getBoneTransform(String boneName) {
        if (boneName.equalsIgnoreCase("neck"))
            return getPlayerLookPBT(0.6f,0.20f);
        if (boneName.equalsIgnoreCase("head"))
            return getPlayerLookPBT(0.6f,0.20f);
        if (boneName.equalsIgnoreCase("spine00"))
            return getPlayerLookPBT(0.10f,0.05f);
        if (boneName.equalsIgnoreCase("spine01"))
            return getPlayerLookPBT(0.10f,0.05f);
        if (boneName.equalsIgnoreCase("L_eye_ctrl"))
            return getPlayerLookPBT(0.25f,0.30f);
        if (boneName.equalsIgnoreCase("R_eye_ctrl"))
            return getPlayerLookPBT(0.25f,0.30f);
        if (boneName.equalsIgnoreCase("eyes_ctrl")) {
            PoseBoneTransform pbt = new PoseBoneTransform();
            pbt.TZ0 = -0.1f;
            return pbt;
        }
        if (boneName.equalsIgnoreCase("leg_L"))
            return getLegTransform(false);
        if (boneName.equalsIgnoreCase("knee_L"))
            return getKneeTransform(false);
        if (boneName.equalsIgnoreCase("ankle_L"))
            return getAnkleTransform(false);
        if (boneName.equalsIgnoreCase("leg_R"))
            return getLegTransform(true);
        if (boneName.equalsIgnoreCase("knee_R"))
            return getKneeTransform(true);
        if (boneName.equalsIgnoreCase("ankle_R"))
            return getAnkleTransform(true);
        if (boneName.equalsIgnoreCase("root")) {
            if (sneakState>0) {
                PoseBoneTransform pbt = new PoseBoneTransform();
                double bob=0.3f+(Math.sin((walking.time)*Math.PI*4)*0.5f);
                bob *= walkingStrengthControl.mulAmount;
                pbt.TZ0 = ((-sneakState) * (1.2f+(float)bob));
                return pbt;
            }
        }
        if (boneName.equalsIgnoreCase("L_longhair_01"))
            return getLonghairTransform(false,0);
        if (boneName.equalsIgnoreCase("R_longhair_01"))
            return getLonghairTransform(true,0);
        if (boneName.equalsIgnoreCase("L_longhair_02"))
            return getLonghairTransform(false,1);
        if (boneName.equalsIgnoreCase("R_longhair_02"))
            return getLonghairTransform(true,1);
        if (boneName.equalsIgnoreCase("L_longhair_03"))
            return getLonghairTransform(false,2);
        if (boneName.equalsIgnoreCase("R_longhair_03"))
            return getLonghairTransform(true,2);
        return null;
    }

    private PoseBoneTransform getLonghairTransform(boolean b,int segment) {
        PoseBoneTransform pbt=new PoseBoneTransform();

        if (segment==0) {
            pbt.Y0=((float)(Math.sin((lookUD)*Math.PI)*0.7f))-0.2f;
            if (pbt.Y0<0)
                pbt.Y0=0;
            if (lookUD<0.226f)
                pbt.Y0=0.257f;
        } else {
            if (segment==1) {
                pbt.Y0 = (float) (Math.sin(lookUD - 0.2f) * 0.7f);
                if (pbt.Y0 > 0)
                    pbt.Y0 = 0;
                if (pbt.Y0 < 0)
                    pbt.Y0 *= -6.0f;
                if (lookUD<0.226f)
                    pbt.Y0 *= 0.8f;
            } else {

            }
        }
        pbt.Y0*=((segment!=0)?-0.25f:1);
        pbt.Y0*=(b?-2:2);
        return pbt;
    }

    private PoseBoneTransform getLegTransform(boolean b) {
        PoseBoneTransform pbt=new PoseBoneTransform();
        if (sneakState<0)
            return null;
        pbt.Y0=(0.5f)*sneakState;
        if (b)
            pbt.Y0=-pbt.Y0;
        return pbt;
    }

    private PoseBoneTransform getKneeTransform(boolean b) {
        PoseBoneTransform pbt=new PoseBoneTransform();
        if (sneakState<0)
            return null;
        pbt.Y0=(-1.0f)*sneakState;
        if (b)
            pbt.Y0=-pbt.Y0;
        return pbt;
    }

    private PoseBoneTransform getAnkleTransform(boolean b) {
        PoseBoneTransform pbt=new PoseBoneTransform();
        if (sneakState<0)
            return null;
        pbt.Y0=(-0.5f)*sneakState;
        if (b)
            pbt.Y0=-pbt.Y0;
        return pbt;
    }

    private PoseBoneTransform getPlayerLookPBT(float lr, float ud) {
        PoseBoneTransform pbt=new PoseBoneTransform();
        pbt.Y1+=lookUD*ud*Math.PI;
        pbt.Z0+=lookLR*lr*Math.PI;
        return pbt;
    }

    @Override
    public void update(double deltaTime) {
        if (sneakState<sneakStateTarget) {
            sneakState += deltaTime * 8.0f;
        } else if (sneakState>sneakStateTarget) {
            sneakState -= deltaTime * 8.0f;
        }
        if (sneakState<-1.0f)
            sneakState=-1.0f;
        if (sneakState>1.0f)
            sneakState=1.0f;

        if (sneakState>-0.1f)
            if (sneakState<0.1f)
                if (sneakStateTarget>-0.1f)
                    if (sneakStateTarget<0.1f)
                        sneakState=sneakStateTarget;

        float speedTarget=(-sneakState)+1.5f;
        if (walking.speed<speedTarget)
            walking.speed+=deltaTime*8.0f;
        if (walking.speed>speedTarget)
            walking.speed-=deltaTime*8.0f;
        if (walking.speed>speedTarget-0.1f)
            if (walking.speed<speedTarget+0.1f)
                walking.speed=speedTarget;
        if (walkingFlag) {
            walkingStrengthControl.mulAmount += deltaTime * 8.0f;
            if (walkingStrengthControl.mulAmount > 1)
                walkingStrengthControl.mulAmount = 1;
        } else {
            walkingStrengthControl.mulAmount -= deltaTime * 8.0f;
            if (walkingStrengthControl.mulAmount < 0) {
                walkingStrengthControl.mulAmount = 0;
                walking.time=0;
            }
        }
        if (fighting.aPos < fightingStateTarget) {
            fighting.aPos += deltaTime * 8.0f;
        } else if (fighting.aPos > fightingStateTarget) {
            fighting.aPos -= deltaTime * 8.0f;
        }
        if (fighting.aPos > -0.1f)
            if (fighting.aPos < 0.1f)
                if (fightingStateTarget > -0.1f)
                    if (fightingStateTarget < 0.1f)
                        fighting.aPos = fightingStateTarget;

        if (fighting.aPos < -1.0f) {
            fighting.aPos = -1.0f;
        }
        if (fighting.aPos > 1.0f) {
            fighting.aPos = 1.0f;
            fightingStateTarget = 0;
        }
    }
}
