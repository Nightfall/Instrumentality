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
package moe.nightfall.instrumentality.editor.controls;

import moe.nightfall.instrumentality.editor.EditElement;

import java.util.HashSet;

/**
 * Created on 09/09/15.
 */
public class TreeviewElement<Node> extends EditElement {
    public final INodeStructurer<Node> nodeStructurer;
    public HashSet<Node> sealedTrees = new HashSet<Node>();
    public int scrollPoint = 0;
    public final ArrowButtonElement upButton, downButton;

    public TreeviewElement(INodeStructurer<Node> ns) {
        nodeStructurer = ns;
        upButton = new ArrowButtonElement(-90, new Runnable() {
            @Override
            public void run() {
                scrollPoint++;
                layout();
            }
        });
        downButton = new ArrowButtonElement(90, new Runnable() {
            @Override
            public void run() {
                scrollPoint--;
                layout();
            }
        });
    }

    public void layout() {
        subElements.clear();
        int buttonH = (getHeight() / 24);
        int depthW = (getWidth() / 16);
        downButton.posX = upButton.posX = getWidth() - depthW;
        upButton.posY = 0;
        downButton.posY = buttonH;
        upButton.setSize(depthW, buttonH);
        downButton.setSize(depthW, buttonH);
        subElements.add(upButton);
        subElements.add(downButton);
        int dp = scrollPoint;
        for (Node n : nodeStructurer.getChildNodes(null)) {
            dp = createElement(dp, 0, n, buttonH, depthW);
        }
    }

    public int createElement(int drawPoint, int depth, final Node n, int buttonH, int depthW) {
        int p = drawPoint + 1;
        if (!sealedTrees.contains(n))
            for (Node n2 : nodeStructurer.getChildNodes(n))
                p = createElement(p, depth + 1, n2, buttonH, depthW);
        // handle children, then:
        TextButtonElement myNode = new TextButtonElement(nodeStructurer.getNodeName(n), new Runnable() {
            @Override
            public void run() {
                nodeStructurer.onNodeClick(n);
            }
        });
        ArrowButtonElement abe = new ArrowButtonElement(sealedTrees.contains(n) ? 0 : 45, new Runnable() {
            @Override
            public void run() {
                if (sealedTrees.contains(n)) {
                    sealedTrees.remove(n);
                } else {
                    sealedTrees.add(n);
                }
                layout();
            }
        });
        abe.posX = depth * depthW;
        abe.posY = drawPoint * buttonH;
        abe.setSize(depthW, buttonH);

        myNode.posX = (depth + 1) * depthW;
        myNode.setSize(getWidth() - (myNode.posX + depthW), buttonH);
        myNode.posY = drawPoint * buttonH;
        if (myNode.posY < 0)
            return p;
        if (myNode.posY > getHeight() - buttonH)
            return p;
        subElements.add(abe);
        subElements.add(myNode);
        return p;
    }

    public interface INodeStructurer<Node> {
        // note: "null" is the Root Node, and is invisible (only it's children are seen)
        String getNodeName(Node n);

        Iterable<Node> getChildNodes(Node n);
        
        void onNodeClick(Node n);
    }
}
