package pl.ivmx.mappum.gui.utils.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.internal.core.JavaModelManager;

import pl.ivmx.mappum.TreeElement;
import pl.ivmx.mappum.gui.model.treeelement.JavaTreeElement;

@SuppressWarnings("restriction")
public class JavaModelGenerator implements IJavaModelGenerator {
	private Logger logger = Logger.getLogger(JavaModelGenerator.class);
	
	private final static Map<String, String> PRIMITIVE_TYPES_MAPPING = new HashMap<String, String>();
	private final static Map<String, String> PRIMITIVE_OBJECTS_MAPPING = new HashMap<String, String>();
	static {

		PRIMITIVE_OBJECTS_MAPPING.put("java.lang.String", "String");
		PRIMITIVE_OBJECTS_MAPPING.put("java.lang.Byte", "Fixnum");
		PRIMITIVE_OBJECTS_MAPPING.put("java.lang.Short", "Fixnum");
		PRIMITIVE_OBJECTS_MAPPING.put("java.lang.Character", "Fixnum");
		PRIMITIVE_OBJECTS_MAPPING.put("java.lang.Integer", "Fixnum");
		PRIMITIVE_OBJECTS_MAPPING.put("java.lang.Long", "Fixnum");
		PRIMITIVE_OBJECTS_MAPPING.put("java.Lang.Float", "Float");
		PRIMITIVE_OBJECTS_MAPPING.put("java.lang.Double", "Float");
		PRIMITIVE_OBJECTS_MAPPING.put("java.util.Date", "Time");
		
		PRIMITIVE_TYPES_MAPPING.put("byte", "Fixnum");
		PRIMITIVE_TYPES_MAPPING.put("short", "Fixnum");
		PRIMITIVE_TYPES_MAPPING.put("char", "Fixnum");
		PRIMITIVE_TYPES_MAPPING.put("int", "Fixnum");
		PRIMITIVE_TYPES_MAPPING.put("long", "Fixnum");
		PRIMITIVE_TYPES_MAPPING.put("float", "Float");
		PRIMITIVE_TYPES_MAPPING.put("double", "Float");
	}

	private static JavaModelGenerator instance;

	public synchronized static JavaModelGenerator getInstance() {
		if (instance == null) {
			instance = new JavaModelGenerator();
		}
		return instance;
	}

	public JavaTreeElement generate(final String classPrefixed, final IProject project)
			throws JavaModelException, IllegalArgumentException {

		JavaTreeElement el = generate0(classPrefixed, null, false, project);
		return el;
	}

	private String getFieldName(final String methodName) {
		final String firstLetter = methodName.substring(3, 4).toLowerCase();
		return firstLetter + methodName.substring(4);
	}
	private boolean isComplex(final String className, final IProject project) throws JavaModelException,
			IllegalArgumentException {
		
		if (PRIMITIVE_TYPES_MAPPING.containsKey(className) ||
				PRIMITIVE_OBJECTS_MAPPING.containsKey(className)) {
			return false;
		}
		
		final IType type = getType(IJavaModelGenerator.JAVA_TYPE_PREFIX + className, project);

		if(type != null){
			for (final IMethod m : type.getMethods()) {
				if (isValidSetter(m) && hasMatchingGetter(type, m)) {
					return true;
				}
			}
		}
		return false;
	}
	private JavaTreeElement generate0(final String classPrefixed, final String name,
			final boolean isArray, final IProject project) throws JavaModelException,
			IllegalArgumentException {
				
		if (!classPrefixed.startsWith(IJavaModelGenerator.JAVA_TYPE_PREFIX)) {
			throw new IllegalArgumentException(String.format(
					"Type name %s must be prefixed with %s.", classPrefixed,
					IJavaModelGenerator.JAVA_TYPE_PREFIX));
		}
		final IType type = getType(classPrefixed, project);
		final List<TreeElement> subElements = new ArrayList<TreeElement>();
		if(type != null){
		for (final IMethod m : type.getMethods()) {
			if (isValidSetter(m) && hasMatchingGetter(type, m)) {

				String parameterType = m.getParameterTypes()[0];
				String flatType;
				boolean isParameterArray;
				if (Signature.getTypeSignatureKind(parameterType) == Signature.ARRAY_TYPE_SIGNATURE) {
					isParameterArray = true;
				} else {
					//For Set and List lets use array
					if ( Signature.getSignatureSimpleName(parameterType).startsWith("Set")) {
						isParameterArray = true;
						parameterType = Signature.getSignatureSimpleName(parameterType);
						parameterType = "L"+parameterType.substring(4,parameterType.length()-1)+";";
					} else if ( Signature.getSignatureSimpleName(parameterType).startsWith("List")) {
						isParameterArray = true;
						parameterType = Signature.getSignatureSimpleName(parameterType);
						parameterType = "L"+parameterType.substring(5,parameterType.length()-1)+";";
					} else {
						isParameterArray = false;
					}
				}
				
				flatType = Signature.getSignatureSimpleName(Signature
						.getElementType(parameterType));

				final String resolved = resolve(type, parameterType);
				if (resolved == null) {
					if (PRIMITIVE_TYPES_MAPPING.containsKey(flatType)) {
						subElements.add(new JavaTreeElement(project,
								PRIMITIVE_TYPES_MAPPING.get(flatType), null,
								isParameterArray, getFieldName(m
										.getElementName())));
					} else {
						throw new IllegalArgumentException(String.format(
								"Parameter type=%s cannot be resolved",
								flatType));
					}
				} else if (PRIMITIVE_OBJECTS_MAPPING.containsKey(resolved)) {
					subElements
							.add(new JavaTreeElement(project, PRIMITIVE_OBJECTS_MAPPING
									.get(resolved), null, isParameterArray,
									getFieldName(m.getElementName())));
				} else {
						String prefixedClass = IJavaModelGenerator.JAVA_TYPE_PREFIX
								+ resolved;
						boolean complex = isComplex(resolved, project);
						
						JavaTreeElement subElem = new JavaTreeElement(project,
								prefixedClass, null, isParameterArray, getFieldName(m
										.getElementName()), complex, complex);

						subElements.add(subElem);
					}
			}
		}
		} else {
			//when type == null
			logger.warn("Type not on classspath:" + classPrefixed + " for element:" + name);
		}
		Collections.sort(subElements);
		final JavaTreeElement el = new JavaTreeElement(project, classPrefixed,
				subElements.isEmpty() ? null : subElements, isArray,
				name != null ? name : type.getElementName(), false, subElements.isEmpty() ? false : true);
		return el;
	}

	private IType getType(final String classPrefixed, final IProject project)
			throws JavaModelException {
		final String classWithoutPrefix = classPrefixed
				.substring(IJavaModelGenerator.JAVA_TYPE_PREFIX.length());

		final IType type = JavaModelManager.getJavaModelManager()
				.getJavaModel().getJavaProject(project.getName()).findType(
						classWithoutPrefix);
		return type;
	}

	private String resolve(final IType type, final String name)
			throws JavaModelException, IllegalArgumentException {
		String[][] resolved = type.resolveType(Signature
				.getSignatureSimpleName(Signature.getElementType(name)));

		if(resolved == null && name.startsWith("L") && name.endsWith(";")) {
			resolved = type.resolveType(name.substring(1,name.length()-1));
		}
		
		if(resolved == null && name.startsWith("L") && name.endsWith(";")) {
			return name.substring(1,name.length()-1);
		}
		
		
		if (resolved == null) {
			return null;
		}

		assert resolved.length == 1;
		assert resolved[0].length == 2;

		return resolved[0][0] + "." + resolved[0][1];
	}

	public static JavaTreeElement findByType(final List<JavaTreeElement> model,
			final String clazz) {
		for (final JavaTreeElement te : model) {
			if (te.getClazz().equals(clazz)) {
				return te;
			}
		}
		return null;
	}

	private boolean hasMatchingGetter(final IType type, final IMethod setter)
			throws JavaModelException {
		assert setter.getNumberOfParameters() == 1;
		for (final IMethod m : type.getMethods()) {
			if (isValidGetter(m)
					&& setter.getElementName().substring(3).equals(
							m.getElementName().substring(3))
					&& setter.getParameterTypes()[0].equals(m.getReturnType())) {
				return true;
			}
		}
		return false;
	}

	private boolean hasValidDeclaration(final IMethod m)
			throws JavaModelException {
		return m.getElementName().length() > 3 && Flags.isPublic(m.getFlags());
	}

	private boolean isValidGetter(final IMethod m) throws JavaModelException {
		return hasValidDeclaration(m) && m.getElementName().startsWith("get");
	}

	private boolean isValidSetter(final IMethod m) throws JavaModelException {
		return hasValidDeclaration(m) && m.getElementName().startsWith("set")
				&& m.getParameterTypes().length == 1
				&& m.getReturnType().equals(String.valueOf(Signature.C_VOID));
	}
}
