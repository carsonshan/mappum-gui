package pl.ivmx.mappum.gui.wizzards;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.jrubyparser.ast.Node;

import pl.ivmx.mappum.gui.model.Connection;
import pl.ivmx.mappum.gui.utils.ModelGenerator;
import pl.ivmx.mappum.gui.utils.RootNodeHolder;

/**
 * Wizard page shown when the user has chosen plane as means of transport
 */

public class ChangeConnectionPropsWizardPage extends WizardPage implements
		Listener {
	// widgets on this page
	private Composite composite;
	private Label sidelabel;
	private Combo sideCombo;
	private Label commentLabel;
	private Text commentText;
	private Label codeLabel;
	private Text codeText;

	private final static String[] mappingSides = {
			Connection.FROM_LEFT_TO_RIGHT_STR,
			Connection.FROM_RIGHT_TO_LEFT_STR, Connection.DUAL_SIDE_STR };

	private int getSide() {
		return ((ChangeConnectionPropsWizard) getWizard()).getMappingSide();
	}

	private String getComment() {
		return ((ChangeConnectionPropsWizard) getWizard()).getComment();
	}

	public String getCode() {
		String code = "";
		// if(code == null){
		try {
			code = ModelGenerator.getInstance().generateRubyCodeFromNode(
					((ChangeConnectionPropsWizard) getWizard())
							.getMappingNode());
			if (code == null || code.equals(""))
				throw new IOException("Parsed code is null");
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "Error while generating code",
					"Error while generating code from ruby tree node");
			e.printStackTrace();
		} catch (CoreException e) {
			MessageDialog.openError(getShell(), "Error while generating code",
					"Error while generating code from ruby tree node");
			e.printStackTrace();
		}
		// }
		return code;
	}

	protected ChangeConnectionPropsWizardPage(String name) {
		super(name);
		setTitle("Mapping properties");
		setDescription("Change mapping side and (optional) comment and code");
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */

	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.numColumns = 2;
		composite.setLayout(gl);
		setControl(composite);

		sidelabel = new Label(composite, SWT.NONE);
		final GridData gd_sidelabel = new GridData(80, SWT.DEFAULT);
		sidelabel.setLayoutData(gd_sidelabel);
		sidelabel.setText("Mapping side:");

		sideCombo = new Combo(composite, SWT.READ_ONLY);
		final GridData gd_combo = new GridData(352, SWT.DEFAULT);
		sideCombo.setLayoutData(gd_combo);
		sideCombo.setItems(mappingSides);
		sideCombo.setText(sideCombo.getItem(getSide()));
		commentLabel = new Label(composite, SWT.NONE);
		commentLabel.setLayoutData(new GridData());
		commentLabel.setText("Comment:");
		new Label(composite, SWT.NONE);

		commentText = new Text(composite, SWT.MULTI | SWT.BORDER);
		commentText.setText(getComment());
		final GridData gd_commentText = new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 2, 1);
		gd_commentText.heightHint = 54;
		gd_commentText.widthHint = 453;
		commentText.setLayoutData(gd_commentText);

		codeLabel = new Label(composite, SWT.NONE);
		codeLabel.setLayoutData(new GridData());
		codeLabel.setText("Code:");
		new Label(composite, SWT.NONE);

		codeText = new Text(composite, SWT.MULTI | SWT.BORDER);
		codeText.setText(getCode());
		final GridData gd_codeText = new GridData(SWT.LEFT, SWT.CENTER, false,
				false, 2, 1);
		gd_codeText.heightHint = 159;
		gd_codeText.widthHint = 453;
		codeText.setLayoutData(gd_codeText);
		addListeners();
		onEnterPage();
		setPageComplete(false);
	}

	private void addListeners() {
		sideCombo.addListener(SWT.Selection, this);
		// commentText.addListener(SWT.Selection, this);
		// codeText.addListener(SWT.Selection, this);
	}

	public boolean canFlipToNextPage() {
		return false;
	}

	/*
	 * Process the events: when the user has entered all information the wizard
	 * can be finished
	 */
	public void handleEvent(Event event) {
		if (event.widget == sideCombo) {
			sideCombo.setEnabled(false);
			String choosenSideStr = sideCombo.getText();
			int newSide;
			if (choosenSideStr.equals(Connection.FROM_LEFT_TO_RIGHT_STR)) {
				newSide = Connection.FROM_LEFT_TO_RIGHT;
			} else if (choosenSideStr.equals(Connection.FROM_RIGHT_TO_LEFT_STR)) {
				newSide = Connection.FROM_RIGHT_TO_LEFT;
			} else {
				newSide = Connection.DUAL_SIDE;
			}
			RootNodeHolder.getInstance()
					.changeMappingAtributes(
							((ChangeConnectionPropsWizard) getWizard())
									.getConnection(),
							Connection.translateSideFromIntToString(newSide),
							null);
			((ChangeConnectionPropsWizard) getWizard()).getConnection()
					.setMappingSide(newSide);
			codeText.setText(getCode());
			codeText.update();
			sideCombo.setEnabled(true);

		}

		setPageComplete(isPageComplete());
		getWizard().getContainer().updateButtons();
	}

	/*
	 * Sets the completed field on the wizard class when all the information is
	 * entered and the wizard can be completed
	 */
	public boolean isPageComplete() {
		// commentText.setText("dupa");
		// commentText.update();
		return true;
	}

	void onEnterPage() {

	}
	public String getRubyCode(){
		return codeText.getText();
	}
	public String getRubyComment() {
		return commentText.getText();
	}


}
