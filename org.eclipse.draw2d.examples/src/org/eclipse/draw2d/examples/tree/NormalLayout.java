package org.eclipse.draw2d.examples.tree;

import java.util.List;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.*;

/**
 * @author hudsonr
 * Created on Apr 22, 2003
 */
class NormalLayout extends BranchLayout {

NormalLayout(TreeBranch branch) {
	super(branch);
}

/**
 * @see org.eclipse.draw2d.examples.tree.BranchLayout#calculateDepth()
 */
void calculateDepth() {
	depth = 0;
	List subtrees = getSubtrees();
	for (int i = 0; i < subtrees.size(); i++)
		depth = Math.max(depth, ((TreeBranch)subtrees.get(i)).getDepth());
	depth++;
}

protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint) {
	Rectangle union = branch.getNodeBounds().getCopy();
	union.union(branch.getContentsPane().getBounds());
	return union.getSize();
}

public void layout(IFigure f) {
	Transposer transposer = getTransposer();
	IFigure contents = branch.getContentsPane();
	IFigure node = branch.getNode();
	contents.validate();

	Point topLeft = transposer.t(branch.getBounds().getTopLeft());
	Rectangle nodeLocation = new Rectangle(topLeft, transposer.t(node.getPreferredSize()));
	nodeLocation.height = rowHeight - getMajorSpacing();
	
	if (contents.getChildren().isEmpty()) {
		node.setBounds(transposer.t(nodeLocation));
		contents.setBounds(
			transposer.t(nodeLocation.getTranslated(0, rowHeight).setSize(0, 0)));
		return;
	}

	Rectangle contentsLocation =
		new Rectangle(topLeft, transposer.t(contents.getPreferredSize()));
	contents.setSize(contents.getPreferredSize());
	contentsLocation.y += rowHeight;

	TreeBranch firstChild = (TreeBranch)contents.getChildren().get(0);
	TreeBranch lastChild =
		(TreeBranch)contents.getChildren().get(contents.getChildren().size() - 1);
	int leftInset =
		firstChild.getContourLeft()[0]
			+ transposer.t(firstChild.getBounds()).x
			- transposer.t(contents.getBounds()).x;
	int rightInset =
		lastChild.getContourRight()[0]
			- transposer.t(lastChild.getBounds()).right()
			+ transposer.t(contents.getBounds()).right();
	int childrenSpan = contentsLocation.width - leftInset - rightInset;

	switch (branch.getAlignment()) {
		case PositionConstants.CENTER :
			leftInset += (childrenSpan - nodeLocation.width) / 2;
	}

	if (leftInset > 0)
		nodeLocation.x += leftInset;
	else
		contentsLocation.x -= leftInset;
	node.setBounds(transposer.t(nodeLocation));
	contents.setBounds(transposer.t(contentsLocation));
}

void mergeContour(int[] destination, int[] source, int startdepth, int offset) {
	for (int i = startdepth; i<source.length; i++)
		destination[i+1] = source[i] + offset;
}

/**
 * @see org.eclipse.draw2d.examples.tree.BranchLayout#paintLines(org.eclipse.draw2d.Graphics)
 */
void paintLines(Graphics g) {
	if (getTransposer().isEnabled()) {
		IFigure node = branch.getNode();
		int left = node.getBounds().right();
		int right = branch.getContentsPane().getBounds().x - 1;
		int yMid = node.getBounds().getCenter().y;
		int xMid = (left + right) / 2;
		List children = getSubtrees();
		if (children.size() == 0)
			return;
		g.drawLine(left, yMid, xMid, yMid);
		int yMin = yMid;
		int yMax = yMid;
		for (int i=0; i<children.size(); i++){
			int y = ((TreeBranch)children.get(i)).getNodeBounds().getCenter().y;
			g.drawLine(xMid, y, right, y);
			yMin = Math.min(yMin, y);
			yMax = Math.max(yMax, y);
		}
		g.drawLine(xMid, yMin, xMid, yMax);

	} else {
		IFigure node = branch.getNode();
		int xMid = node.getBounds().getCenter().x;
		int top = node.getBounds().bottom();
		int bottom = branch.getContentsPane().getBounds().y - 1;
		int yMid = (top + bottom) / 2;
		List children = getSubtrees();
		if (children.size() == 0)
			return;
		g.drawLine(xMid, top, xMid, yMid);
		int xMin = xMid;
		int xMax = xMid;
		for (int i=0; i<children.size(); i++){
			int x = ((TreeBranch)children.get(i)).getNodeBounds().getCenter().x;
			g.drawLine(x, yMid, x, bottom);
			xMin = Math.min(xMin, x);
			xMax = Math.max(xMax, x);
		}
		g.drawLine(xMin, yMid, xMax, yMid);
	}
}

/**
 * @see org.eclipse.draw2d.examples.tree.BranchLayout#updateContours()
 */
void updateContours() {
	Transposer transposer = getTransposer();
	//Make sure we layout first
	branch.validate();

	cachedContourLeft = new int[getDepth()];
	cachedContourRight = new int[getDepth()];

	Rectangle clientArea = transposer.t(branch.getClientArea(Rectangle.SINGLETON));
	Rectangle nodeBounds = transposer.t(branch.getNodeBounds());
	Rectangle contentsBounds = transposer.t(branch.getContentsPane().getBounds());
	
	cachedContourLeft[0] = nodeBounds.x - clientArea.x;
	cachedContourRight[0] = clientArea.right() - nodeBounds.right();

	List subtrees = getSubtrees();
	TreeBranch subtree;

	int currentDepth = 0;
	for (int i = 0; i < subtrees.size() && currentDepth < getDepth(); i++) {
		subtree = (TreeBranch)subtrees.get(i);
		if (subtree.getDepth() > currentDepth) {
			int leftContour[] = subtree.getContourLeft();
			int leftOffset = transposer.t(subtree.getBounds()).x - contentsBounds.x;
			mergeContour(cachedContourLeft, leftContour, currentDepth, leftOffset);
			currentDepth = subtree.getDepth();
		}
	}

	currentDepth = 0;
	for (int i = subtrees.size() - 1; i >= 0 && currentDepth < getDepth(); i--) {
		subtree = (TreeBranch)subtrees.get(i);
		if (subtree.getDepth() > currentDepth) {
			int rightContour[] = subtree.getContourRight();
			int rightOffset =
				contentsBounds.right() - transposer.t(subtree.getBounds()).right();
			mergeContour(cachedContourRight, rightContour, currentDepth, rightOffset);
			currentDepth = subtree.getDepth();
		}
	}
}

/**
 * @see org.eclipse.draw2d.examples.tree.BranchLayout#updateRowHeights()
 */
void updateRowHeights() {
	Transposer transposer = getTransposer();
	preferredRowHeights = new int[getDepth()];
	List subtrees = getSubtrees();
	preferredRowHeights[0] =
		transposer.t(branch.getNode().getPreferredSize()).height + getMajorSpacing();
	TreeBranch subtree;
	
	for (int i = 0; i < subtrees.size(); i++) {
		subtree = (TreeBranch)subtrees.get(i);
		int rowHeights[] = subtree.getPreferredRowHeights();
		for (int row = 0; row < rowHeights.length; row++)
			preferredRowHeights[row + 1] =
				Math.max(preferredRowHeights[row + 1], rowHeights[row]);
	}
}

/**
 * @see org.eclipse.draw2d.examples.tree.BranchLayout#setRowHeights(int[], int)
 */
void setRowHeights(int[] heights, int offset) {
	super.setRowHeights(heights, offset);
	List subtrees = getSubtrees();
	offset++;
	for (int i=0; i<subtrees.size(); i++)
		((TreeBranch)subtrees.get(i)).setRowHeights(heights, offset);
}

}
