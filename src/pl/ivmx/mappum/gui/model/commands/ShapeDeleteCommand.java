package pl.ivmx.mappum.gui.model.commands;

import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.commands.Command;

import pl.ivmx.mappum.gui.model.Connection;
import pl.ivmx.mappum.gui.model.Shape;

public class ShapeDeleteCommand extends Command {
	/** Shape to remove. */
	private final Shape child;

	/** ShapeDiagram to remove from. */
	private final Shape parent;
	/** Holds a copy of the outgoing connections of child. */
	private List<Connection> sourceConnections;
	/** Holds a copy of the incoming connections of child. */
	private List<Connection> targetConnections;
	/** True, if child was removed from its parent. */
	private boolean wasRemoved;

	/**
	 * Create a command that will remove the shape from its parent.
	 * 
	 * @param parent
	 *            the ShapesDiagram containing the child
	 * @param child
	 *            the Shape to remove
	 * @throws IllegalArgumentException
	 *             if any parameter is null
	 */
	public ShapeDeleteCommand(Shape parent, Shape child) {
		if (parent == null || child == null) {
			throw new IllegalArgumentException();
		}
		setLabel("shape deletion");
		this.parent = parent;
		this.child = child;
	}

	/**
	 * Reconnects a List of Connections with their previous endpoints.
	 * 
	 * @param connections
	 *            a non-null List of connections
	 */
	private void addConnections(final List<Connection> connections) {
		for (final Iterator<Connection> iter = connections.iterator(); iter
				.hasNext();) {
			Connection conn = iter.next();
			conn.reconnect();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#canUndo()
	 */
	public boolean canUndo() {
		return wasRemoved;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		// store a copy of incoming & outgoing connections before proceeding
		sourceConnections = child.getSourceConnections();
		targetConnections = child.getTargetConnections();
		redo();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo() {
		// remove the child and disconnect its connections
		wasRemoved = parent.removeChild(child);
		if (wasRemoved) {
			removeConnections(sourceConnections);
			removeConnections(targetConnections);
		}
	}

	/**
	 * Disconnects a List of Connections from their endpoints.
	 * 
	 * @param connections
	 *            a non-null List of connections
	 */
	private void removeConnections(final List<Connection> connections) {
		for (final Iterator<Connection> iter = connections.iterator(); iter
				.hasNext();) {
			Connection conn = iter.next();
			conn.disconnect();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		// add the child and reconnect its connections
		if (parent.addChild(child)) {
			addConnections(sourceConnections);
			addConnections(targetConnections);
		}
	}
}