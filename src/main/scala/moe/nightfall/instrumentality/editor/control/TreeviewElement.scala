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

import moe.nightfall.instrumentality.editor.EditElement
import scala.collection.immutable
import moe.nightfall.instrumentality.editor.UIUtils

class TreeviewElement[Node](ns: TreeviewElementStructurer[Node]) extends EditElement {
    val nodeStructurer = ns

    var sealedTrees = new immutable.HashSet[Node]
    var selectedNode: Node = _

    override def layout() {
        subElements.clear()

        var buttonH = 24
        var depthW = 24
        var dp, numNodes, width = 0
        
        for (node <- nodeStructurer.getChildNodes(None)) {
            dp = createElement(dp, 0, node)
        }
        
        sizeWidth = width
        sizeHeight = numNodes * buttonH
        
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
            myNode.baseStrength = if (node == selectedNode) 0.25f else 0.5f
    
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
            myNode.setSize(math.ceil(UIUtils.sizeLine(nodeName).getX * 1.5f).toInt, buttonH)
            
            width = math.max(width, myNode.width + myNode.posX)
    
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
