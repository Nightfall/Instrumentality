package moe.nightfall.instrumentality.editor.control

import java.util

import moe.nightfall.instrumentality.editor.EditElement

import scala.collection.immutable

class TreeviewElement[Node](ns: TreeviewElementStructurer[Node]) extends EditElement {
	val nodeStructurer: TreeviewElementStructurer[Node] = ns
	
	var sealedTrees: immutable.HashSet[Node] = new immutable.HashSet[Node]
	var scrollPoint: Int = 0
	
	var selectedNode: Node = _
	
	val upButton: ArrowButtonElement = new ArrowButtonElement(-90, () => {
		scrollPoint += 1
		layout()
	})
	val downButton: ArrowButtonElement = new ArrowButtonElement(+90, () => {
		scrollPoint -= 1
		layout()
	})
	
	override def layout() {
		subElements.clear()
		val buttonH = height / 12
		val depthW = width / 16
		
		upButton.posX = width - depthW
		downButton.posX = upButton.posX
		
		upButton.posY = 0
		downButton.posY = buttonH
		
		upButton.setSize(depthW, buttonH)
		downButton.setSize(depthW, buttonH)
		
		subElements ++= Array(upButton, downButton
		
		var dp = scrollPoint
		
		for(node <- nodeStructurer.getChildNodes(None)) {
			dp = createElement(dp, 0, node, buttonH, depthW)
		}
	}
	
	def createElement(drawPoint: Int, depth: Int, node: Node, buttonH: Int, depthW: Int): Int = {
		var p = drawPoint + 1
		
		// TODO: Remove recursion!!!
		if (!sealedTrees.contains(node))
			for (node2 <- nodeStructurer.getChildNodes(Option(node)))
				p = createElement(p, depth + 1, node2, buttonH, depthW)
		
		val myNode = new TextButtonElement(nodeStructurer.getNodeName(Option(node)), () => {
			selectedNode = node
			nodeStructurer.onNodeClick(Option(node))
			layout()
		})
		myNode.baseStrength = if (node == selectedNode) 0.25f else 0.5f
		
		val arrowButtonElement = new ArrowButtonElement(if (sealedTrees.contains(node)) 0 else 45, () => {
			if (sealedTrees.contains(node))
				sealedTrees -= node
			else sealedTrees += node
			layout()
		})
		
		arrowButtonElement.posX = depth * depthW
		arrowButtonElement.posY = drawPoint * buttonH
		arrowButtonElement.setSize(width - (myNode.posX + depthW), buttonH)
		myNode.posY = drawPoint * buttonH
		
		if (myNode.posY < 0) return p
		if (myNode.posY > height - buttonH) return p
		
		subElements ++= Array(arrowButtonElement, myNode)
		/*return*/ p
	}
}

trait TreeviewElementStructurer[Node] {
	// note: "null" is the Root Node, and is invisible (only it's children are seen)
	def getNodeName(n: Option[Node]): String
	def getChildNodes(n: Option[Node]): Iterable[Node]
	def onNodeClick(n: Option[Node])
}
