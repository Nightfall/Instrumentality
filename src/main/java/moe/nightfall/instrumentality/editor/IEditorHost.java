package moe.nightfall.instrumentality.editor;

/**
 * Created on 18/08/15.
 */
public interface IEditorHost {
    /**
     * Called to change a panel.
     * Must call EditElement.setSize for setting the size, and call EditElement.cleanup on the old element.
     *
     * @param newPanel The new root element/panel.
     */
    void changePanel(EditElement newPanel);
}
