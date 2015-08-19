package moe.nightfall.instrumentality.mc.gui;

import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.IEditorHost;
import moe.nightfall.instrumentality.editor.MainFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

/**
 * Created on 18/08/15.
 */
public class EditorHostGui extends GuiScreen implements IEditorHost {

    public EditElement hostedElement;

    public EditorHostGui() {
    }

    @Override
    public void initGui() {
        changePanel(new MainFrame());
    }

    @Override
    public void setWorldAndResolution(Minecraft p_146280_1_, int p_146280_2_, int p_146280_3_) {
        super.setWorldAndResolution(p_146280_1_, p_146280_2_, p_146280_3_);
        hostedElement.setSize(width, height);
    }

    @Override
    public void drawScreen(int par1, int par2, float par3) {
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        // do gui-y stuff
        hostedElement.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    @Override
    public void changePanel(EditElement newPanel) {
        if (hostedElement != null)
            hostedElement.cleanup();
        hostedElement = newPanel;
        hostedElement.setSize(width, height);
    }
}
