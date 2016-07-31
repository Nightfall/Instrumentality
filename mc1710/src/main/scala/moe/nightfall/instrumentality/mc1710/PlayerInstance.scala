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
package moe.nightfall.instrumentality.mc1710

import moe.nightfall.instrumentality.animations.{AnimSet, NewPCAAnimation}
import moe.nightfall.instrumentality.{Loader, PMXInstance, PMXModel}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.{DynamicTexture, ITextureObject}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraft.world.{EnumSkyBlock, World}
import org.lwjgl.opengl.GL11

class PlayerInstance(file: PMXModel, animSet: AnimSet) {

    var anim: NewPCAAnimation = null

    // CONST
    val pmxInst = new PMXInstance(file)
    anim = new NewPCAAnimation(animSet)
    pmxInst.anim = anim

    var clippingPoint = 0d

    /**
     * Miku's hair position.
     */
    var hairSway = 0.5f

    /**
     * Miku's hair velocity.
     * This is not directly modified by the head movement, instead it changes hairSway.
     */
    var hairSwayVel = 0.0f

    /**
     * Used to work out how much Miku has rotated
     */
    var lastTotalRotation = 0.0f

    /**
     * Used to work out how much Miku's head has rotated
     */
    var lastLRValue = 0.0d

    /**
     * Set to 1.0f for sneaking,-1.0f for flying
     */
    var sneakStateTarget = 0f
    var sneakState = 0f

    /**
     * This is used so Miku's feet don't magically turn along with her body.
     */
    var directionAdjustment = 0.0f

    var daTarget = 0.0f

    var resetFrame = true

    def update(deltaTime: Double) {
        val sChange: Float = deltaTime.toFloat * 8.0f
        if (sneakStateTarget == 0) {
            if (sneakState < 0)
                if (sneakState + sChange > 0)
                    sneakState = 0
            if (sneakState > 0)
                if (sneakState - sChange < 0)
                    sneakState = 0
        }

        if (sneakState < sneakStateTarget) {
            sneakState += sChange
        } else if (sneakState > sneakStateTarget) {
            sneakState -= sChange
        }
        if (sneakState < 0.0f)
            sneakState = 0.0f
        if (sneakState > 1.0f)
            sneakState = 1.0f

        // Time to calculate Miku's "hair physics"
        hairSwayVel += ((-hairSway) * deltaTime * 4.0f).toFloat
        hairSway += (hairSwayVel * deltaTime * 4.0f).toFloat
        hairSwayVel -= (hairSwayVel * (deltaTime / 1.0f)).toFloat

        pmxInst.update(deltaTime)
        clippingPoint += deltaTime / 2.0d
        if (clippingPoint >= 1.1d)
            clippingPoint = 1.1d

        if ((anim.walkStrengthTarget > 0) || (sneakState < 0)) {
            val waChange: Float = (math.Pi * deltaTime * 2).toFloat
            if (directionAdjustment < daTarget) {
                directionAdjustment += waChange
                if (directionAdjustment > daTarget)
                    directionAdjustment = daTarget
            } else {
                directionAdjustment -= waChange
                if (directionAdjustment < daTarget)
                    directionAdjustment = daTarget
            }
        }
    }

    private def interpolate(a: Float, b: Float, point: Float) = a + ((b - a) * point)

    var lastKnownLightmap = 0

    def findCol(worldObj: World, x: Int, y: Int, z: Int) = {
        // This is probably going to be hell to translate to a newer MC version, so to explain:
        val sky = worldObj.getSkyBlockTypeBrightness(EnumSkyBlock.Sky, x, y, z)
        val block = worldObj.getSkyBlockTypeBrightness(EnumSkyBlock.Block, x, y, z)
        // sky and block are now positions in the DYNAMIC lightmap texture (the ordinary lightmap texture doesn't account for a lot of things)

        var p: ITextureObject = null
        while (p == null) {
            p = Minecraft.getMinecraft.renderEngine.getTexture(new ResourceLocation("dynamic/lightMap_" + lastKnownLightmap))
            lastKnownLightmap = lastKnownLightmap + 1
        }
        lastKnownLightmap = lastKnownLightmap - 1
        Minecraft.getMinecraft.renderEngine
        // pull out the correct colour out of the lightmap
        val a = p.asInstanceOf[DynamicTexture].getTextureData
        val col = a(block + (sky * 16))
        val colR = (col & 0xFF0000) >> 16
        val colG = (col & 0xFF00) >> 8
        val colB = col & 0xFF
        (colR / 255f, colG / 255f, colB / 255f)
    }

    def interpolCol2(col_m: (Float, Float, Float), col_c: (Float, Float, Float), d: Double) = (interpolate(col_m._1, col_c._1, d.toFloat), interpolate(col_m._2, col_c._2, d.toFloat), interpolate(col_m._3, col_c._3, d.toFloat))

    def interpolCol(col_m: (Float, Float, Float), col_c: (Float, Float, Float), col_p: (Float, Float, Float), d: Double) = if (d < 0.5) interpolCol2(col_m, col_c, d * 2) else interpolCol2(col_c, col_p, (d - 0.5) * 2)

    def render(player: EntityPlayer, x: Double, y: Double, z: Double, partialTick: Float, firstPerson: Boolean) {
        val adjustFactor = if (player.isSneaking()) 0.14f else 0.07f
        val scale = 1F / (pmxInst.theModel.height / player.height)

        val rotBody = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTick)

        GL11.glPushMatrix()
        if (firstPerson) {
            // we need it to be at 0,0,0, and perfectly scaled,
            // so that firstPerson applies perfectly
            GL11.glLoadIdentity()
            val scale = 1F / pmxInst.theModel.height
            GL11.glScalef(scale, scale, scale)
        } else {
            GL11.glTranslated(x, ((y - player.height) + player.eyeHeight) + adjustFactor, z)
            GL11.glRotated(180 - rotBody, 0, 1, 0)
            GL11.glScalef(scale, scale, scale)
            GL11.glRotated(math.toDegrees(directionAdjustment), 0, 1, 0)
        }
        // I fixed the triangle order(I think. Or was that the -X bias, which would mean it's now broken again?), but skirts do not play well with culling
        GL11.glDisable(GL11.GL_CULL_FACE)

        // Try to find the best colour. (If I wasn't suffering a terminal case of Can't Be Bothered, this could be done on a per-vertex or even per-pixel basis)
        val col_c = findCol(player.worldObj, math.floor(player.posX).toInt, math.floor(player.posY).toInt, math.floor(player.posZ).toInt)
        val col_xM = findCol(player.worldObj, math.floor(player.posX - 1).toInt, math.floor(player.posY).toInt, math.floor(player.posZ).toInt)
        val col_xP = findCol(player.worldObj, math.floor(player.posX + 1).toInt, math.floor(player.posY).toInt, math.floor(player.posZ).toInt)
        val col_yM = findCol(player.worldObj, math.floor(player.posX).toInt, math.floor(player.posY - 1).toInt, math.floor(player.posZ).toInt)
        val col_yP = findCol(player.worldObj, math.floor(player.posX).toInt, math.floor(player.posY + 1).toInt, math.floor(player.posZ).toInt)
        val col_zM = findCol(player.worldObj, math.floor(player.posX).toInt, math.floor(player.posY).toInt, math.floor(player.posZ - 1).toInt)
        val col_zP = findCol(player.worldObj, math.floor(player.posX).toInt, math.floor(player.posY).toInt, math.floor(player.posZ + 1).toInt)
        val col_x = interpolCol(col_xM, col_c, col_xP, player.posX - math.floor(player.posX))
        val col_y = interpolCol(col_yM, col_c, col_yP, player.posY - math.floor(player.posY))
        val col_z = interpolCol(col_zM, col_c, col_zP, player.posZ - math.floor(player.posZ))

        val col_f = ((col_x._1 + col_y._1 + col_z._1) / 3, (col_x._2 + col_y._2 + col_z._2) / 3, (col_x._3 + col_y._3 + col_z._3) / 3)
        // Everything but the division is done...

        var renderClip = clippingPoint
        anim.firstPerson = firstPerson
        pmxInst.render(col_f._1, col_f._2, col_f._3, renderClip.toFloat, 0.25f)

        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glPopMatrix()
    }

    def apply(player: EntityPlayer, partialTick: Float) {

        val pitch = interpolate(player.prevRotationPitch, player.rotationPitch, partialTick)
        val rotBody = interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTick)

        anim.lookUD = (-pitch / 90.0f).toFloat

        sneakStateTarget = 0
        if (player.isSneaking())
            sneakStateTarget = 1

        /*
         * Documentation on what this does:
         * bodyRotation is where MC wants root to be facing(and where Vic's code right now makes us face)
         * directionAdjustment is basically "how much are we working against bodyRotation".
         * hairSwayVel is adjusted with the data we get, though that needs to be changed to use bodyRotation+directionAdjustment+lookDir
         * lookLR is the final look value
         * lookDir is the input look value
         */

        var bRotation = math.toRadians(rotBody)
        // Normalize between 0 and PI*2
        while (bRotation < 0)
            bRotation += math.Pi * 2
        while (bRotation > (math.Pi * 2))
            bRotation -= math.Pi * 2
        var distRotation = angleDist(bRotation.toFloat, lastTotalRotation)
        lastTotalRotation = bRotation.toFloat

        if (resetFrame)
            distRotation = 0

        directionAdjustment += distRotation
        while (directionAdjustment < -Math.PI)
            directionAdjustment += (Math.PI * 2).toFloat
        while (directionAdjustment > Math.PI)
            directionAdjustment -= (Math.PI * 2).toFloat

        var lookDir = Math.toRadians(player.rotationYawHead).toFloat

        daTarget = ((-lookDir) + bRotation).toFloat
        while (daTarget < -Math.PI)
            daTarget += (Math.PI * 2).toFloat
        while (daTarget > Math.PI)
            daTarget -= (Math.PI * 2).toFloat

        if (resetFrame)
            directionAdjustment = daTarget

        var finalRot = (-bRotation) + directionAdjustment

        finalRot = lookDir + finalRot

        while (finalRot < -Math.PI)
            finalRot += Math.PI * 2
        while (finalRot > Math.PI)
            finalRot -= Math.PI * 2

        anim.lookLR = (-finalRot / Math.PI).toFloat

        val lrValue = anim.lookLR * 1.2f
        hairSwayVel += (distRotation / 2.0f) + ((lrValue - lastLRValue) * 2).toFloat
        lastLRValue = lrValue

        resetFrame = false

        if (player.isSwingInProgress) {
            if (anim.swingTime == -1)
                anim.swingTime = 0
        } else {
            anim.swingTime = -1
        }

        var spdMul = 0d
        if ((player.lastTickPosX != player.posX) || (player.lastTickPosZ != player.posZ)) {
            val dX = Math.abs(player.lastTickPosX - player.posX)
            val dZ = Math.abs(player.lastTickPosZ - player.posZ)
            val dist = Math.sqrt((dX * dX) + (dZ * dZ))
            spdMul = dist * 10.0d
        }
        anim.walkAnimation.speed = spdMul
        anim.walkStrengthTarget = Math.min(1.0d, spdMul * 10.0d)
        val fallVel = player.lastTickPosY - player.posY
        anim.fallStrength = fallVel
        anim.itemHoldStrengthTarget = if (player.getHeldItem == null) 0 else 1
    }

    def angleDist(totalRotation: Float, lastTotalRotation: Float): Float = {
        var a = totalRotation - lastTotalRotation
        var b = 0f
        if (totalRotation < lastTotalRotation) {
            // given a TR of 0.20, a LTR of 0.80 and a L of 1:
            // TR+(1-LTR)=0.40
            b = -adSpecial(totalRotation, lastTotalRotation)
        } else {
            // given a LTR of 0.20, a TR of 0.80 and a L of 1:
            // LTR+(1-TR)=0.80
            b = adSpecial(lastTotalRotation, totalRotation)
        }
        if (math.abs(a) > math.abs(b))
            return b
        return a
    }

    def adSpecial(lower: Float, upper: Float): Float = (lower + ((math.Pi * 2) - upper)).toFloat

    def cleanupGL() = pmxInst.cleanupGL()
}
