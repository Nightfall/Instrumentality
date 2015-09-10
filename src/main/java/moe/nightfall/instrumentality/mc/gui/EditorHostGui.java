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
package moe.nightfall.instrumentality.mc.gui;

import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.UIFont;
import moe.nightfall.instrumentality.editor.UIUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/**
 * Created on 18/08/15.
 */
public class EditorHostGui extends GuiScreen {

    public EditElement hostedElement;

    public EditorHostGui() {
    }

    @Override
    public void initGui() {
        if (UIFont.fontDB == null)
            try {
                UIFont.setFont(Minecraft.getMinecraft().mcDefaultResourcePack.getInputStream(new ResourceLocation("instrumentality:/font.txt")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        if (hostedElement == null)
            changePanel(UIUtils.createGui());
    }

    @Override
    public void setWorldAndResolution(Minecraft p_146280_1_, int p_146280_2_, int p_146280_3_) {
        super.setWorldAndResolution(p_146280_1_, p_146280_2_, p_146280_3_);
        if (hostedElement != null)
            hostedElement.setSize(Display.getWidth(), Display.getHeight());
    }

    @Override
    public void drawScreen(int par1, int par2, float par3) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, Display.getWidth(), Display.getHeight(), 0, 0, 1024);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        hostedElement.draw(Display.getWidth(), Display.getHeight());
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void updateScreen() {
        UIUtils.update(hostedElement);
        hostedElement.update(0.05d);
    }

    public void changePanel(EditElement newPanel) {
        if (hostedElement != null)
            hostedElement.cleanup();
        hostedElement = newPanel;
        hostedElement.setSize(width, height);
    }
}
