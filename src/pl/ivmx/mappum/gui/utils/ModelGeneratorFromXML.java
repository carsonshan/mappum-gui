package pl.ivmx.mappum.gui.utils;

import java.util.List;

import javax.script.ScriptException;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.jrubyparser.SourcePosition;
import org.jrubyparser.ast.CallNode;
import org.jrubyparser.ast.DVarNode;
import org.jrubyparser.ast.ListNode;

import pl.ivmx.mappum.MappumApi;
import pl.ivmx.mappum.TreeElement;
import pl.ivmx.mappum.WorkdirLoader;
import pl.ivmx.mappum.gui.model.Shape;
import pl.ivmx.mappum.gui.model.Shape.Side;

public class ModelGeneratorFromXML {
	public static final String LANGUAGE_RUBY = "jruby";

	public static final String DEFAULT_MAP_FOLDER = ".map";
	public static final String DEFAULT_WORKING_MAP_FOLDER = "map";
	public static final String DEFAULT_SCHEMA_FOLDER = "schema";
	public static final String DEFAULT_GENERATED_CLASSES_FOLDER = ".classes";
	public static final String DEFAULT_PROJECT_PROPERTIES_FILE = "project.properties";

	private String mapFolder;
	private String schemaFolder;
	private String generatedClassesFolder;

	private List<TreeElement> model;

	private Logger logger = Logger.getLogger(ModelGeneratorFromXML.class);

	private static final ModelGeneratorFromXML INSTANCE = new ModelGeneratorFromXML();

	private ModelGeneratorFromXML() {
	}

	public static final ModelGeneratorFromXML getInstance() {
		return INSTANCE;
	}

	public String getMapFolder() {
		return mapFolder;
	}

	public void setMapFolder(String mapFolder) {
		this.mapFolder = mapFolder;
	}

	public String getSchemaFolder() {
		return schemaFolder;
	}

	public void setSchemaFolder(String schemaFolder) {
		this.schemaFolder = schemaFolder;
	}

	public String getGeneratedClassesFolder() {
		return generatedClassesFolder;
	}

	public void setGeneratedClassesFolder(String generatedClassesFolder) {
		this.generatedClassesFolder = generatedClassesFolder;
	}

	private List<TreeElement> evalMappumApi() {
		MappumApi mp = new MappumApi();
		WorkdirLoader wl = mp.getWorkdirLoader(getSchemaFolder(),
				getMapFolder(), getGeneratedClassesFolder());
		wl.generateAndRequire();
		return wl.definedElementTrees();
	}

	public List<TreeElement> getModel() throws ScriptException {
		if (model == null) {
			if ((model = evalMappumApi()) == null)
				throw new ScriptException(
						"Error while returning model. Model is null. Check logs for more details.");
		}
		return model;
	}

	public void setModel(List<TreeElement> model) {
		this.model = model;
	}

	public List<TreeElement> generateModel(IProject project)
			throws ScriptException {
		final String classesFolder = project.getFolder(
				ModelGeneratorFromXML.DEFAULT_GENERATED_CLASSES_FOLDER)
				.getLocation().toPortableString();
		final String mapFolder = project.getFolder(
				ModelGeneratorFromXML.DEFAULT_MAP_FOLDER).getLocation()
				.toPortableString();
		final String schemaFolder = project.getFolder(
				ModelGeneratorFromXML.DEFAULT_SCHEMA_FOLDER).getLocation()
				.toPortableString();

		setGeneratedClassesFolder(classesFolder);
		setMapFolder(mapFolder);
		setSchemaFolder(schemaFolder);

		return getModel();
	}

	private Shape checkAndAddShape(String name, Shape parent, Side side,
			boolean isArray) {
		if (parent == null) {
			if (side == Shape.Side.LEFT) {
				if (name.equals(Shape.getRootShapes().get(0).getFullName())) {
					return Shape.getRootShapes().get(0);
				} else {
					throw new IllegalArgumentException(
							"There is no root element for arguments: Shape name: "
									+ name + ", side: " + side);
				}
			} else {
				if (name.equals(Shape.getRootShapes().get(1).getFullName())) {
					return Shape.getRootShapes().get(1);
				} else {
					throw new IllegalArgumentException(
							"There is no root element for arguments: Shape name: "
									+ name + ", side: " + side);
				}
			}
		} else {
			for (Shape shape : parent.getChildren()) {
				if (shape.getName().equals(name)) {
					return shape;
				}
			}
			Shape shape = Shape.createShape(name, null, parent, side,
					generateRubyModelForField(name, side));
			shape.setArrayType(isArray);
			shape.addToParent();
			return shape;
		}
	}

	public void addFieldsFromRubyModel(String leftElement, String rightElement) {
		for (TreeElement element : model) {
			if (element.getName().equals(leftElement)) {
				Shape parent = checkAndAddShape(leftElement, null,
						Shape.Side.LEFT, false);
				for (TreeElement childElement : element.getElements()) {
					Shape child = checkAndAddShape(childElement.getName(),
							parent, Shape.Side.LEFT, childElement.getIsArray());
					if (childElement.getClazz() != null) {
						getComplexField(childElement.getClazz(), child,
								Shape.Side.LEFT);
					}
				}
			}
			if (element.getName().equals(rightElement)) {
				Shape parent = checkAndAddShape(rightElement, null,
						Shape.Side.RIGHT, false);
				for (TreeElement childElement : element.getElements()) {
					Shape child = checkAndAddShape(childElement.getName(),
							parent, Shape.Side.RIGHT, childElement.getIsArray());
					if (childElement.getClazz() != null) {
						getComplexField(childElement.getClazz(), child,
								Shape.Side.RIGHT);
					}
				}
			}
		}
	}

	private void getComplexField(String searchElement, Shape parent,
			final Shape.Side side) {
		for (TreeElement element : model) {
			if (element.getName().equals(searchElement)) {
				for (TreeElement childElement : element.getElements()) {
					Shape child = checkAndAddShape(childElement.getName(),
							parent, side, childElement.getIsArray());
					if (childElement.getClazz() != null) {
						getComplexField(childElement.getClazz(), child, side);
					}
				}
			}
		}
	}

	private CallNode generateRubyModelForField(String name, final Shape.Side side) {
		// String prefix = RootNodeHolder.getInstance().generateRandomIdent(
		// RootNodeHolder.IDENT_LENGTH);
		if (side == Shape.Side.LEFT) {
			ListNode listNode = new ListNode(new SourcePosition());
			DVarNode dVarNode = new DVarNode(new SourcePosition(), 0,
					"changeMe");
			return new CallNode(new SourcePosition(), dVarNode, name, listNode);
		} else {
			ListNode listNode = new ListNode(new SourcePosition());
			DVarNode dVarNode = new DVarNode(new SourcePosition(), 1,
					"changeMe");
			return new CallNode(new SourcePosition(), dVarNode, name, listNode);
		}

	}
}
