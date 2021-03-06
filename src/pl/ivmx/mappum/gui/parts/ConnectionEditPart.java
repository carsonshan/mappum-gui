package pl.ivmx.mappum.gui.parts;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MidpointLocator;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ConnectionEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import pl.ivmx.mappum.gui.IMappumEditor;
import pl.ivmx.mappum.gui.model.Connection;
import pl.ivmx.mappum.gui.model.ModelElement;
import pl.ivmx.mappum.gui.model.commands.ConnectionDeleteCommand;
import pl.ivmx.mappum.gui.wizzards.ChangeConnectionPropsWizard;

class ConnectionEditPart extends AbstractConnectionEditPart implements
		PropertyChangeListener {

	private final IMappumEditor editor;

	public ConnectionEditPart(final IMappumEditor editor) {
		this.editor = editor;
	}

	/**
	 * Upon activation, attach to the model element as a property change
	 * listener.
	 */
	public void activate() {
		if (!isActive()) {
			super.activate();
			((ModelElement) getModel()).addPropertyChangeListener(this);

			getViewer().select(this);

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
	 */
	protected void createEditPolicies() {
		// Selection handle edit policy.
		// Makes the connection show a feedback, when selected by the user.
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
		// Allows the removal of the connection model element
		installEditPolicy(EditPolicy.CONNECTION_ROLE,
				new ConnectionEditPolicy() {
					protected Command getDeleteCommand(GroupRequest request) {
						return new ConnectionDeleteCommand(getCastedModel());
					}
				});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
	 */
	protected IFigure createFigure() {
		final PolylineConnection figure = (PolylineConnection) super
				.createFigure();
		final Connection connection = getCastedModel();

		if (connection.getConnectionType() == Connection.Type.CONST_TO_VAR_CONN) {
			Label label = new Label("CONSTANT: " + connection.getConstantName());
			figure.add(label, new MidpointLocator(figure, 0));
			figure.setLineStyle(Graphics.LINE_DOT);
		} else if (connection.getConnectionType() == Connection.Type.FUN_TO_VAR_CONN) {
			String functions = "FUNCTIONS: ";
			for (String function : connection.getFunctions()) {
				functions = functions + function + ", ";
			}
			String substring = functions.substring(0, functions.length() - 2);
			Label label = new Label(substring);
			figure.add(label, new MidpointLocator(figure, 0));
			figure.setLineStyle(Graphics.LINE_DASH);
		} else if (connection.getConnectionType() == Connection.Type.VAR_TO_VAR_CONN
				&& connection.getArrayNumber() > -1) {
			Label label = new Label("[" + connection.getArrayNumber() + "]");
			figure.add(label, new MidpointLocator(figure, 0));
		}
		if (connection.getMappingSide() == Connection.Side.DUAL) {
			figure.setTargetDecoration(new PolygonDecoration()); // arrow at
			// target
			// endpoint
			figure.setSourceDecoration(new PolygonDecoration());
		} else if (connection.getMappingSide() == Connection.Side.LEFT_TO_RIGHT) {
			figure.setTargetDecoration(new PolygonDecoration());
		} else {
			figure.setSourceDecoration(new PolygonDecoration());
		}
		figure.setToolTip(new Label(connection.getMappingCode()));

		return figure;
	}

	/**
	 * Upon deactivation, detach from the model element as a property change
	 * listener.
	 */
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			((ModelElement) getModel()).removePropertyChangeListener(this);
		}
	}

	private Connection getCastedModel() {
		return (Connection) getModel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seejava.beans.PropertyChangeListener#propertyChange(java.beans.
	 * PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getPropertyName();
		if (Connection.MAPPING_PROP.equals(property)) {
			if (getCastedModel().getMappingSide() == Connection.Side.DUAL) {
				((PolylineConnection) getFigure())
						.setTargetDecoration(new PolygonDecoration());
				((PolylineConnection) getFigure())
						.setSourceDecoration(new PolygonDecoration());
			} else if (getCastedModel().getMappingSide() == Connection.Side.LEFT_TO_RIGHT) {
				((PolylineConnection) getFigure())
						.setTargetDecoration(new PolygonDecoration());
				((PolylineConnection) getFigure()).setSourceDecoration(null);
			} else {
				((PolylineConnection) getFigure()).setTargetDecoration(null);
				((PolylineConnection) getFigure())
						.setSourceDecoration(new PolygonDecoration());
			}
		} else if (Connection.COMMENT_PROP.equals(property)) {
			
		}
	}

	private int openChangeConnectionPropsWizard() {

		Connection connection = getCastedModel();
		Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
		ChangeConnectionPropsWizard wizard = new ChangeConnectionPropsWizard(
				connection, editor);
		wizard.init();
		WizardDialog dialog = new WizardDialog(shell, wizard);
		return dialog.open();
	}

	public void performRequest(Request req) {
		if (req.getType().equals(RequestConstants.REQ_OPEN))
			System.out.println("double-click");
		final int res = openChangeConnectionPropsWizard();
		if (res == SWT.OK) {
			editor.reload();
		}
	}
}