package pl.ivmx.mappum.gui.wizzards;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import pl.ivmx.mappum.gui.utils.ModelGeneratorFromXML;
import pl.ivmx.mappum.gui.utils.ProjectProperties;

public class NewProjectWizard extends Wizard implements INewWizard,
		IExecutableExtension {
	private NewProjectWizardPage page;
	private Logger logger = Logger.getLogger(NewProjectWizard.class);
	private IConfigurationElement configElement;

	public NewProjectWizard() {
		super();
	}

	private IFolder createFolderHandle(IPath folderPath) {

		return ResourcesPlugin.getWorkspace().getRoot().getFolder(folderPath);
	}

	@Override
	public boolean performFinish() {
		IProject projectHandle = page.getProjectHandle();
		try {
			projectHandle.create(null);
			projectHandle.open(null);
		} catch (CoreException e) {
			logger.error("Error performing finish operations: "
					+ e.getCause().getMessage());
		}

		IPath projectPath = projectHandle.getFullPath();
		IPath folderMapPath = projectPath
				.append(ModelGeneratorFromXML.DEFAULT_MAP_FOLDER);
		IPath folderWorkingMapPath = projectPath
				.append(ModelGeneratorFromXML.DEFAULT_WORKING_MAP_FOLDER);
		IPath folderSchemaPath = projectPath
				.append(ModelGeneratorFromXML.DEFAULT_SCHEMA_FOLDER);
		IPath folderClassesPath = projectPath
				.append(ModelGeneratorFromXML.DEFAULT_GENERATED_CLASSES_FOLDER);
		IFolder folderMap = createFolderHandle(folderMapPath);
		IFolder folderWorkingMap = createFolderHandle(folderWorkingMapPath);
		IFolder folderSchema = createFolderHandle(folderSchemaPath);
		IFolder folderClasses = createFolderHandle(folderClassesPath);
		try {
			folderMap.create(false, true, null);
			folderWorkingMap.create(false, true, null);
			folderSchema.create(false, true, null);
			folderClasses.create(false, true, null);
		} catch (CoreException e) {
			logger.error("Error performing finish operations: "
					+ e.getCause().getMessage());
		}
		ProjectProperties properties = new ProjectProperties(projectHandle);
		properties.setProperty(ProjectProperties.CLASSES_DIRECTORY_PROPS,
				projectHandle.getLocation().append(
						ModelGeneratorFromXML.DEFAULT_GENERATED_CLASSES_FOLDER)
						.toPortableString());
		properties.setProperty(ProjectProperties.MAP_DIRECTORY_PROPS,
				projectHandle.getLocation().append(
						ModelGeneratorFromXML.DEFAULT_MAP_FOLDER)
						.toPortableString());
		properties.setProperty(ProjectProperties.SCHEMA_DIRECTORY_PROPS,
				projectHandle.getLocation().append(
						ModelGeneratorFromXML.DEFAULT_SCHEMA_FOLDER)
						.toPortableString());
		properties.setProperty(ProjectProperties.WORKING_MAP_DIRECTORY_PROPS,
				projectHandle.getLocation().append(
						ModelGeneratorFromXML.DEFAULT_WORKING_MAP_FOLDER)
						.toPortableString());

		logger.debug("New Project wizzard ended. Created new mappum project: "
				+ projectHandle.getName());
		BasicNewProjectResourceWizard.updatePerspective(configElement);

		addJavaNature(projectHandle);
		return true;
	}

	private void addJavaNature(final IProject project) {
		try {
			final IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = "org.eclipse.jdt.core.javanature";
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		} catch (final CoreException e) {

		}
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		logger.debug("New Project wizzard started.");
		setWindowTitle("New Mappum Project Wizard"); // NON-NLS-1
		setNeedsProgressMonitor(true);
		page = new NewProjectWizardPage("New Mappum Project Wizard");
	}

	public void addPages() {
		super.addPages();
		addPage(page);
	}

	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		this.configElement = config;

	}

}
