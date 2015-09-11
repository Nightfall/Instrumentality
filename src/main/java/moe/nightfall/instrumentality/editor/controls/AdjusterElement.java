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

/**
 * Created on 10/09/15.
 */
public class AdjusterElement extends EditElement {
    public ArrowButtonElement incButton;
    public ArrowButtonElement decButton;
    public LabelElement valueDisplay;
    public String valuePrefix = "";
    public IAdjustable adjustable;
    public double adjustmentValue = 0.05d;

    public AdjusterElement(String vp) {
        valuePrefix = vp;
        decButton = new ArrowButtonElement(180, new Runnable() {
            @Override
            public void run() {
                adjustable.setValue(correct(adjustable.getValue() - adjustmentValue));
            }
        });
        incButton = new ArrowButtonElement(0, new Runnable() {
            @Override
            public void run() {
                adjustable.setValue(correct(adjustable.getValue() + adjustmentValue));
            }
        });
        valueDisplay = new LabelElement("?");
        subElements.add(decButton);
        subElements.add(incButton);
        subElements.add(valueDisplay);
    }

    /**
     * Keeps the value from succumbing to rounding errors
     *
     * @param v The value to correct
     * @return The value, rounded to the nearest adjustmentvalue
     */
    private double correct(double v) {
        return Math.round(v * 100) / 100d;
    }

    @Override
    public void draw(int scrWidth, int scrHeight) {
        valueDisplay.theText = valuePrefix + adjustable.getValue();
        super.draw(scrWidth, scrHeight);
    }

    @Override
    public void layout() {
        super.layout();
        int du = getHeight();
        decButton.posX = 0;
        decButton.posY = 0;
        decButton.setSize(du, du);
        incButton.posX = getWidth() - du;
        incButton.posY = 0;
        incButton.setSize(du, du);
        valueDisplay.posX = du;
        valueDisplay.posY = 0;
        valueDisplay.setSize(getWidth() - (du * 2), du);
    }

    public interface IAdjustable {
        double getValue();

        void setValue(double v);
    }
}
