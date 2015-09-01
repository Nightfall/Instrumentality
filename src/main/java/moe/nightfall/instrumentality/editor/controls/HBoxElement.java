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

import java.util.LinkedList;

/**
 * See: GTK+ v3's horizontal Box for a general idea of what this is supposed to be used for.
 * It's just less flexible.
 * Created on 01/09/15.
 */
public class HBoxElement extends EditElement {
    private LinkedList<EditElement> barPieces = new LinkedList<EditElement>();

    public void addPiece(EditElement editElement) {
        barPieces.add(editElement);
        subElements.add(editElement);
        layout();
    }

    public void layout() {
        if (barPieces.size() == 0)
            return;
        int div = getWidth() / barPieces.size();
        int leftover = getWidth() - (div * barPieces.size());
        boolean first = true;
        int pos = 0;
        for (EditElement button : barPieces) {
            int ds = div;
            if (first)
                ds += leftover;
            button.posX = pos;
            button.posY = 0;
            pos += ds;
            button.setSize(ds, getHeight());
            first = false;
        }
    }
}
