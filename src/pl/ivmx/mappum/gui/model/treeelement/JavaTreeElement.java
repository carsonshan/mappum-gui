package pl.ivmx.mappum.gui.model.treeelement;

import java.util.List;

import pl.ivmx.mappum.TreeElement;

public class JavaTreeElement extends TreeElementAdapter {

	public JavaTreeElement(final String clazz,
			final List<TreeElement> elements, final boolean isArray,
			final String name) {
		this(clazz, elements, isArray, name, true, false);
	}

	public JavaTreeElement(final String clazz,
			final List<TreeElement> elements, final boolean isArray,
			final String name, final boolean complete,
			final boolean markAsComplex) {
		this.clazz = clazz;
		this.elements = elements;
		this.isArray = isArray;
		this.name = name;
		this.complete = complete;
		this.markAsComplex = markAsComplex;
	}

	private final String clazz;
	private final List<TreeElement> elements;
	private final boolean isArray;
	private final String name;
	private final boolean complete;
	private final boolean markAsComplex;

	@Override
	public String getClazz() {
		return clazz;
	}

	@Override
	public List<TreeElement> getElements() {
//Better just return null		
//		if (!isComplete()) {
//			throw new IllegalStateException(
//					"Cannot get children of incomplete element");
//		}
		return elements;
	}

	@Override
	public boolean getIsArray() {
		return isArray;
	}

	@Override
	public String getName() {
		return name;
	}

	public boolean isComplete() {
		return complete;
	}

	public boolean isMarkedAsComplex() {
		return markAsComplex;
	}

	@Override
	public String toString() {
		return String.format(
				"[clazz=%s;isArray=%b;name=%s;elements=%s;complete=%b]", clazz,
				isArray, name, elements, complete);
	}
}