package moe.nightfall.instrumentality.editor.guis;

import java.util.LinkedList;

import org.lwjgl.opengl.GL11;

import moe.nightfall.instrumentality.Loader;
import moe.nightfall.instrumentality.PMXFile;
import moe.nightfall.instrumentality.PMXFile.PMXBone;
import moe.nightfall.instrumentality.PMXInstance;
import moe.nightfall.instrumentality.PMXModel;
import moe.nightfall.instrumentality.animations.PoseAnimation;
import moe.nightfall.instrumentality.editor.EditElement;
import moe.nightfall.instrumentality.editor.controls.TreeviewElement;
import moe.nightfall.instrumentality.editor.controls.View3DElement;

public class PoseEditElement extends EditElement {
    public PoseAnimation editedPose;
    public PMXInstance pmxInst;
    public View3DElement model;
    public TreeviewElement<PMXFile.PMXBone> tView;
    public int selectedBoneId=0;
    
    public PoseEditElement(PoseAnimation ep, PMXModel pm) {
        pmxInst=new PMXInstance(pm);
        pmxInst.anim=ep;
        editedPose=ep;
        model=new View3DElement() {
            @Override
            protected void draw3d() {
                GL11.glPushMatrix();
                GL11.glTranslated(0, -0.5, 0);
                GL11.glBegin(GL11.GL_LINE_STRIP);
                GL11.glColor3d(0.5, 0.5, 0.5);
                GL11.glVertex3d(-0.5, 0, -0.5);
                GL11.glVertex3d(0.5, 0, -0.5);
                GL11.glVertex3d(0.5, 0, 0.5);
                GL11.glVertex3d(-0.5, 0, 0.5);
                GL11.glVertex3d(-0.5, 0, -0.5);
                GL11.glEnd();
                GL11.glBegin(GL11.GL_LINES);
                GL11.glColor3d(0.5, 0.5, 0.5);
                GL11.glVertex3d(0, 0, -0.5);
                GL11.glVertex3d(0, 0, 0.5);
                GL11.glVertex3d(-0.5, 0, 0);
                GL11.glVertex3d(0.5, 0, 0);
                GL11.glEnd();
                double s=1/pmxInst.theModel.height;
                GL11.glScaled(s, s, s);
                pmxInst.renderDebug(selectedBoneId);
                GL11.glPopMatrix();
            }};
        subElements.add(model);
        tView=new TreeviewElement<PMXFile.PMXBone>(new TreeviewElement.INodeStructurer<PMXFile.PMXBone>() {

            @Override
            public String getNodeName(PMXFile.PMXBone n) {
                return n.globalName;
            }

            @Override
            public Iterable<PMXFile.PMXBone> getChildNodes(PMXFile.PMXBone n) {
                int parId=-1;
                if (n!=null)
                    parId=n.boneId;
                LinkedList<PMXFile.PMXBone> ll=new LinkedList<PMXFile.PMXBone>();
                for (PMXFile.PMXBone pb : pmxInst.theFile.boneData)
                    if (pb.parentBoneIndex==parId)
                        ll.add(pb);
                return ll;
            }

            @Override
            public void onNodeClick(PMXBone n) {
                selectedBoneId=n.boneId;
            }
        });
        subElements.add(tView);
    }
    
    @Override
    public void layout() {
        model.posX=0;
        model.posY=0;
        int hSplit=(int)(getWidth()*0.40d);
        int vSplit=(int)(getHeight()*0.75d);
        model.setSize(hSplit, getHeight());
        tView.posX=hSplit;
        tView.posY=0;
        tView.setSize(getWidth()-hSplit, vSplit);
    }
}
