package org.eclipse.gef.editpolicies;
/*
 * Licensed Material - Property of IBM
 * (C) Copyright IBM Corp. 2001, 2002 - All Rights Reserved.
 * US Government Users Restricted Rights - Use, duplication or disclosure
 * restricted by GSA ADP Schedule Contract with IBM Corp.
 */

import java.util.List;


import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.*;

import org.eclipse.gef.*;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.requests.*;

/**
 * For use with <code>LayoutManager</code> that require a <i>constraint</i>.
 * ConstraintedLayoutEditPolicy understands {@link RequestConstants#REQ_ALIGN_CHILDREN}
 * in addition to the Requests handled in the superclass.
 * @author hudsonr
 * @since 2.0 */
public abstract class ConstrainedLayoutEditPolicy
	extends LayoutEditPolicy
{

/**
 * Returns the <code>Command</code> to perform an Add with the specified child and
 * constraint. The constraint has been converted from a draw2d constraint to an object
 * suitable for the model by calling {@link #translateToModelConstraint(Object)}.
 * @param child the EditPart of the child being added * @param constraint the model constraint, after being {@link
 * #translateToModelConstraint(Object) translated}
 * @return the Command to add the child */
protected abstract Command createAddCommand(
	EditPart child,
	Object constraint);

/**
 * Returns the <code>Command</code> to change the specified child's constraint. The
 * constraint has been converted from a draw2d constraint to an object suitable for the
 * model
 * @param child the EditPart of the child being changed * @param constraint the new constraint, after being {@link
 * #translateToModelConstraint(Object) translated} * @return Command */
protected abstract Command createChangeConstraintCommand(
	EditPart child,
	Object constraint);

/**
 * Overrides <code>getAddCommand()</code> to generate the proper constraint for each child
 * being added. Once the constraint is calculated, {@link
 * #createAddCommand(EditPart,Object)} is called. Subclasses must implement this method.
 * @see org.eclipse.gef.editpolicies.LayoutEditPolicy#getAddCommand(Request) */
protected Command getAddCommand(Request generic) {
	ChangeBoundsRequest request = (ChangeBoundsRequest)generic;
	List editParts = request.getEditParts();
	CompoundCommand command = new CompoundCommand();
	command.setDebugLabel("Add in ConstrainedLayoutEditPolicy");//$NON-NLS-1$
	GraphicalEditPart childPart;
	Rectangle r;
	Object constraint;

	for (int i = 0; i < editParts.size(); i++) {
		childPart = (GraphicalEditPart)editParts.get(i);
		r = childPart.getFigure().getBounds().getCopy();
		//convert r to absolute from childpart figure
		childPart.getFigure().translateToAbsolute(r);
		r = request.getTransformedRectangle(r);
		//convert this figure to relative 
		getLayoutContainer().translateToRelative(r);
		getLayoutContainer().translateFromParent(r);
		r.translate(getLayoutOrigin().getNegated());
		constraint = getConstraintFor(r);
		command.add(createAddCommand(childPart,
			translateToModelConstraint(constraint)));
	}
	return command.unwrap();
}

/**
 * Returns the command to align a group of children. By default, this is treated the same
 * as a resize, and {@link #getResizeChildrenCommand(ChangeBoundsRequest)} is returned.
 * @param request the AligmentRequest * @return the command to perform aligment */
protected Command getAlignChildrenCommand(AlignmentRequest request) {
	return getResizeChildrenCommand(request);
}

/**
 * Factors out RESIZE and ALIGN requests, otherwise calls <code>super</code>.
 * @see org.eclipse.gef.EditPolicy#getCommand(Request) */
public Command getCommand(Request request) {
	if (REQ_RESIZE_CHILDREN.equals(request.getType()))
		return getResizeChildrenCommand((ChangeBoundsRequest)request);
	if (REQ_ALIGN_CHILDREN.equals(request.getType()))
		return getAlignChildrenCommand((AlignmentRequest)request);

	return super.getCommand(request);
}

/**
 * Generates a draw2d constraint object derived from the specified child EditPart using
 * the provided Request. The returned constraint will be translated to the application's
 * model later using {@link #translateToModelConstraint(Object)}.
 * @param request the ChangeBoundsRequest
 * @param child the child EditPart for which the constraint should be generated
 * @return the draw2d constraint
 */
protected Object getConstraintFor (ChangeBoundsRequest request, GraphicalEditPart child) {
	Rectangle rect = child.getFigure().getBounds();
	rect = request.getTransformedRectangle(rect);
	rect.translate(getLayoutOrigin().getNegated());
	return getConstraintFor(rect);
}

/**
 * Generates a draw2d constraint given a <code>Point</code>. This method is called during
 * creation, when only a mouse location is available.
 * @param point the Point relative to the {@link #getLayoutOrigin() layout origin}
 * @return the constraint
 */
protected abstract Object getConstraintFor (Point point);

/**
 * Generates a draw2d constraint given a <code>Rectangle</code>. This method is called
 * during most operations.
 * @param rect the Rectangle relative to the {@link #getLayoutOrigin() layout origin}
 * @return the constraint
 */
protected abstract Object getConstraintFor (Rectangle rect);

/**
 * Generates a draw2d constraint for the given <code>CreateRequest</code>. If the
 * CreateRequest has a size, {@link #getConstraintFor(Rectangle)} is called with a
 * Rectangle of that size and the result is returned. This is used during size-on-drop
 * creation. Otherwise, {@link #getConstraintFor(Point)} is returned.
 * <P>
 * The CreateRequest's location is relative the Viewer. The location is made
 * layout-relative before calling one of the methods mentioned above.
 * @param request the CreateRequest * @return a draw2d constraint */
protected Object getConstraintFor(CreateRequest request) {
	IFigure figure = getLayoutContainer();

	Point where = request.getLocation().getCopy();
	Dimension size = request.getSize();

	figure.translateToRelative(where);
	figure.translateFromParent(where);
	where.translate(getLayoutOrigin().getNegated());

	if (size == null || size.isEmpty())
		return getConstraintFor(where);
	else
		return getConstraintFor(new Rectangle(where, size));
}

/**
 * Converts a constraint from the format used by LayoutManagers,
 * to the form stored in the model.
 * @param figureConstraint the draw2d constraint
 * @return the model constraint
 */
protected Object translateToModelConstraint(Object figureConstraint) {
	return figureConstraint;
}

/**
 * Returns the <code>Command</code> to resize a group of children.
 * @param request the ChangeBoundsRequest * @return the Command */
protected Command getResizeChildrenCommand(ChangeBoundsRequest request) {
	CompoundCommand resize = new CompoundCommand();
	Command c;
	GraphicalEditPart child;
	List children = request.getEditParts();

	for (int i = 0; i < children.size(); i++) {
		child = (GraphicalEditPart)children.get(i);
		c = createChangeConstraintCommand(child,
			translateToModelConstraint(
				getConstraintFor(request, child)));
		resize.add(c);
	}
	return resize.unwrap();
}

/**
 * Returns the <code>Command</code> to move a group of children. By default, move is
 * treated the same as a resize.
 * @see org.eclipse.gef.editpolicies.LayoutEditPolicy#getMoveChildrenCommand(Request) */
protected Command getMoveChildrenCommand(Request request) {
	//By default, move and resize are treated the same for constrained layouts.
	return getResizeChildrenCommand((ChangeBoundsRequest)request);
}

/**
 * Returns the layout's origin relative to the {@link
 * LayoutEditPolicy#getLayoutContainer()}. In other words, what Point on the parent Figure
 * does the LayoutManager use a reference when generating the child figure's bounds from
 * the child's constraint.
 * <P>
 * By default, it is assumed that the layout manager positions children relative to the
 * client area of the layout container. Thus, when processing Viewer-relative Points or
 * Rectangles, the clientArea's location (top-left corner) will be subtracted from the
 * Point/Rectangle, resulting in an offset from the LayoutOrigin.
 * @return Point */
protected Point getLayoutOrigin() {
	return getLayoutContainer().getClientArea().getLocation();
}

}