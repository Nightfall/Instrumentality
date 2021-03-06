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
package moe.nightfall.instrumentality.editor.control

import moe.nightfall.instrumentality.editor.{SelfResizable, EditElement, UIUtils}
import org.lwjgl.util.vector.Vector2f
import scala.collection.immutable
import org.lwjgl.opengl.GL11

class TreeviewElement[Node](ns: TreeviewElementStructurer[Node]) extends EditElement with SelfResizable {
    val depthW = 32
    val buttonH = 32
    var numNodes = 1
    var maxWidth = 1

    var suggestionChanged = true
    var doneCull = false

    /**
     * Dramatically speeds up layout.
     * As this cache is connected to the TreeviewElement, it dies with it - good to keep performance high!
     */
    var textSizeCache = Map[String, Vector2f]()

    override def getSuggestedSize: (Int, Int) = {
        (maxWidth, buttonH * numNodes)
    }

    override def hasSuggestionChanged: Boolean = suggestionChanged

    /**
     * Sets the "suggestion changed" flag to false.
     * Perform this after the setSize in response to the suggestion.
     */
    override def clearSuggestionChanged = suggestionChanged = false

    val nodeStructurer = ns

    var sealedTrees = new immutable.HashSet[Node]
    var selectedNode: Node = _

    override def draw() {
        if (!doneCull) {
            subElements.filter((e) => drawWillCull(e)._1).foreach(subElements -= _)
            doneCull = true
        }
        // Do not draw a containing panel.
        drawSubelements()
    }

    override def layout() {
        subElements.clear()
        doneCull = false

        var dp = 0

        numNodes = 0
        maxWidth = depthW

        for (node <- nodeStructurer.getChildNodes(None)) {
            dp = createElement(dp, 0, node)
        }

        suggestionChanged = true

        def createElement(drawPoint: Int, depth: Int, node: Node): Int = {

            // increment node pointer
            numNodes += 1
            
            var p = drawPoint + 1
    
            val childNodes = nodeStructurer.getChildNodes(Option(node));
            if (!sealedTrees.contains(node))
                for (node2 <- childNodes)
                    p = createElement(p, depth + 1, node2)
    
            val nodeName = nodeStructurer.getNodeName(node)

            val myNode = new TextButtonElement(nodeName, {
                selectedNode = node
                nodeStructurer.onNodeClick(node)
                layout()
            })
            myNode.baseStrength = if (node == selectedNode) 0.9f else 1.0f
    
            val subElemSize = subElements.size
            val arrowButtonElement = new ArrowButtonElement(if (sealedTrees.contains(node)) 0 else 45, {
                if (sealedTrees.contains(node))
                    sealedTrees -= node
                else sealedTrees += node
                layout()
            })
    
            arrowButtonElement.posX = depth * depthW
            arrowButtonElement.posY = drawPoint * buttonH
            arrowButtonElement.setSize(depthW, buttonH)
    
            myNode.posX = (depth + 1) * depthW
            myNode.posY = drawPoint * buttonH
            if (childNodes.isEmpty)
                myNode.posX -= depthW


            val textsize = if (textSizeCache.contains(nodeName)) textSizeCache(nodeName)
            else {
                val v = UIUtils.sizeLine(nodeName)
                textSizeCache += nodeName -> v
                v
            }
            val textsizemul = (buttonH - myNode.borderWidth) / textsize.getY
            myNode.setSize(math.ceil(textsize.getX * textsizemul).toInt, buttonH)

            val endDepth = (depth * depthW) + myNode.width + depthW
            if (maxWidth < endDepth)
                maxWidth = endDepth

            if (myNode.posY < 0) return p
            if (myNode.posY > height - buttonH) return p
    
            if (childNodes.isEmpty) {
                subElements += myNode
            } else {
                subElements += arrowButtonElement += myNode
            }
    
            return p
        }
    }
}

trait TreeviewElementStructurer[Node] {
    // note: None is the Root Node, and is invisible (only it's children are seen)
    // As only the children are seen, any function about seeing or clicking obviously can't receive it

    def getNodeName(n: Node): String

    def getChildNodes(n: Option[Node]): Iterable[Node]

    def onNodeClick(n: Node)
}
