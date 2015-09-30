package moe.nightfall.instrumentality.editor.control

import moe.nightfall.instrumentality.editor.EditElement

import scala.collection.immutable

class TreeviewElement[Node](ns: TreeviewElementStructurer[Node]) extends EditElement {
    val nodeStructurer: TreeviewElementStructurer[Node] = ns

    var sealedTrees: immutable.HashSet[Node] = new immutable.HashSet[Node]
    var scrollPoint: Int = 0

    var selectedNode: Node = _

    val upButton: ArrowButtonElement = new ArrowButtonElement(-90, {
        scrollPoint += 1
        layout()
    })
    val downButton: ArrowButtonElement = new ArrowButtonElement(+90, {
        scrollPoint -= 1
        layout()
    })

    override def layout() {
        subElements.clear()

        var buttonH = height / 12.0
        if (buttonH > 24)
            buttonH = buttonH / ((((buttonH / 24.0) - 1) / 2) + 1)
        val depthW = width / 16.0

        upButton.posX = (width - depthW).toInt
        downButton.posX = upButton.posX

        upButton.posY = 0
        downButton.posY = buttonH.toInt

        upButton.setSize(depthW.toInt, buttonH.toInt)
        downButton.setSize(depthW.toInt, buttonH.toInt)

        subElements ++= Array(upButton, downButton)

        var dp = scrollPoint

        for (node <- nodeStructurer.getChildNodes(None)) {
            dp = createElement(dp, 0, node, buttonH.toInt, depthW.toInt)
        }
    }

    def createElement(drawPoint: Int, depth: Int, node: Node, buttonH: Int, depthW: Int): Int = {
        var p = drawPoint + 1

        val childNodes = nodeStructurer.getChildNodes(Option(node));
        if (!sealedTrees.contains(node))
            for (node2 <- childNodes)
                p = createElement(p, depth + 1, node2, buttonH, depthW)

        val myNode = new TextButtonElement(nodeStructurer.getNodeName(node), {
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
        myNode.setSize(width - (myNode.posX + depthW), buttonH)

        if (myNode.posY < 0) return p
        if (myNode.posY > height - buttonH) return p

        if (childNodes.isEmpty) {
            subElements ++= Array(myNode)
        } else {
            subElements ++= Array(arrowButtonElement, myNode)
        }

        /*return*/ p
    }
}

trait TreeviewElementStructurer[Node] {
    // note: None is the Root Node, and is invisible (only it's children are seen)
    // As only the children are seen, any function about seeing or clicking obviously can't receive it

    def getNodeName(n: Node): String

    def getChildNodes(n: Option[Node]): Iterable[Node]

    def onNodeClick(n: Node)
}
