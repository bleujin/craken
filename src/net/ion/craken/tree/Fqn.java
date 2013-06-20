package net.ion.craken.tree;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.ObjectUtil;

import org.infinispan.marshall.AbstractExternalizer;
import org.infinispan.util.ReflectionUtil;
import org.infinispan.util.Util;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

// @Immutable
public class Fqn implements Comparable<Fqn>, Serializable {

	private static final long serialVersionUID = 7459897811324670392L;
	public static final String SEPARATOR = "/";

	private final Object[] elements;
	private transient int hash_code = 0;

	/**
	 * Immutable root Fqn.
	 */
	public static final Fqn ROOT = new Fqn();

	/**
	 * A cached string representation of this Fqn, used by toString to it isn't calculated again every time.
	 */
	protected String stringRepresentation;
	private static final Object[] EMPTY_ARRAY = ReflectionUtil.EMPTY_CLASS_ARRAY;

	// ----------------- START: Private constructors for use by factory methods only. ----------------------

	private Fqn(Object... elements) {
		this.elements = elements;
	}

	/**
	 * If safe is false, Collections.unmodifiableList() is used to wrap the list passed in. This is an optimisation so Fqn.fromString(), probably the most frequently used factory method, doesn't end up needing to use the unmodifiableList() since it creates the list internally.
	 * 
	 * @param names
	 *            List of names
	 */
	private Fqn(List<?> names) {
		if (names != null)
			elements = names.toArray();
		else
			elements = EMPTY_ARRAY;
	}

	private Fqn(Fqn base, Object... relative) {
		elements = new Object[base.elements.length + relative.length];
		System.arraycopy(base.elements, 0, elements, 0, base.elements.length);
		System.arraycopy(relative, 0, elements, base.elements.length, relative.length);
	}

	// ----------------- END: Private constructors for use by factory methods only. ----------------------


	@SuppressWarnings("unchecked")
	public static Fqn fromList(List<?> names) {
		return new Fqn(names);
	}


	public static Fqn fromElements(Object... elements) {
		Object[] copy = new Object[elements.length];
		System.arraycopy(elements, 0, copy, 0, elements.length);
		return new Fqn(copy);
	}


	public static Fqn fromRelativeFqn(Fqn base, Fqn relative) {
		return new Fqn(base, relative.elements);
	}


	public static Fqn fromRelativeList(Fqn base, List<?> relativeElements) {
		return new Fqn(base, relativeElements.toArray());
	}


	public static Fqn fromRelativeElements(Fqn base, Object... relativeElements) {
		return new Fqn(base, relativeElements);
	}


	public static Fqn fromString(String stringRepresentation) {
		if (stringRepresentation == null || stringRepresentation.equals(SEPARATOR) || stringRepresentation.length() == 0)
			return root();

		String toMatch = stringRepresentation.startsWith(SEPARATOR) ? stringRepresentation.substring(1) : stringRepresentation;
		String[] el = toMatch.split(SEPARATOR);
//		return new Fqn(el) ;
		return new Fqn(Iterables.toArray(Splitter.on(SEPARATOR).trimResults().omitEmptyStrings().split(toMatch), String.class));
	}


	public Fqn getAncestor(int generation) {
		if (generation == 0)
			return root();
		return getSubFqn(0, generation);
	}


	public Fqn getSubFqn(int startIndex, int endIndex) {
		if (endIndex < startIndex)
			throw new IllegalArgumentException("End index cannot be less than the start index!");
		int len = endIndex - startIndex;
		Object[] el = new Object[len];
		System.arraycopy(elements, startIndex, el, 0, len);
		return new Fqn(el);
	}


	public int size() {
		return elements.length;
	}


	public Object get(int n) {
		return elements[n];
	}


	public JsonPrimitive toJson(){
		return new JsonPrimitive(toString()) ;
	}
	
	
	public Object getLastElement() {
		if (isRoot())
			return null;
		return elements[elements.length - 1];
	}


	public boolean hasElement(Object element) {
		return indexOf(element) != -1;
	}

	private int indexOf(Object element) {
		if (element == null) {
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] == null)
					return i;
			}
		} else {
			for (int i = 0; i < elements.length; i++) {
				if (element.equals(elements[i]))
					return i;
			}
		}
		return -1;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Fqn)) {
			return false;
		}
		Fqn other = (Fqn) obj;
		if (elements.length != other.elements.length)
			return false;
		for (int i = elements.length - 1; i >= 0; i--) {
			if (!Util.safeEquals(elements[i], other.elements[i]))
				return false;
		}
		return true;
	}


	@Override
	public int hashCode() {
		if (hash_code == 0) {
			hash_code = calculateHashCode();
		}
		return hash_code;
	}


	@Override
	public String toString() {
		if (stringRepresentation == null) {
			stringRepresentation = getStringRepresentation(elements);
		}
		return stringRepresentation;
	}


	public boolean isChildOf(Fqn parentFqn) {
		return parentFqn.elements.length != elements.length && isChildOrEquals(parentFqn);
	}

	/**
	 * Returns true if this Fqn is a <i>direct</i> child of a given Fqn.
	 * 
	 * @param parentFqn
	 *            parentFqn to compare with
	 * @return true if this is a direct child, false otherwise.
	 */
	public boolean isDirectChildOf(Fqn parentFqn) {
		return elements.length == parentFqn.elements.length + 1 && isChildOf(parentFqn);
	}


	public boolean isChildOrEquals(Fqn parentFqn) {
		Object[] parentEl = parentFqn.elements;
		if (parentEl.length > elements.length) {
			return false;
		}
		for (int i = parentEl.length - 1; i >= 0; i--) {
			if (!Util.safeEquals(parentEl[i], elements[i]))
				return false;
		}
		return true;
	}


	protected int calculateHashCode() {
		int hashCode = 19;
		for (Object o : elements)
			hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());
		if (hashCode == 0)
			hashCode = 0xDEADBEEF; // degenerate case
		return hashCode;
	}

	protected String getStringRepresentation(Object[] elements) {
		StringBuilder builder = new StringBuilder();
		for (Object e : elements) {
			// incase user element 'e' does not implement equals() properly, don't rely on their implementation.
			if (!SEPARATOR.equals(e) && !"".equals(e)) {
				builder.append(SEPARATOR);
				builder.append(e);
			}
		}
		return builder.length() == 0 ? SEPARATOR : builder.toString();
	}


	public Fqn getParent() {
		switch (elements.length) {
		case 0:
		case 1:
			return root();
		default:
			return getSubFqn(0, elements.length - 1);
		}
	}

	public static Fqn root() // declared final so compilers can optimise and in-line.
	{
		return ROOT;
	}


	public boolean isRoot() {
		return elements.length == 0;
	}


	public String getLastElementAsString() {
		if (isRoot()) {
			return SEPARATOR;
		} else {
			Object last = getLastElement();
			if (last instanceof String)
				return (String) last;
			else
				return String.valueOf(getLastElement());
		}
	}


	public List<Object> peekElements() {
		return Arrays.asList(elements);
	}


	@Override
	public int compareTo(Fqn fqn) {
		return FqnComparator.INSTANCE.compare(this, fqn);
	}


	public Fqn replaceAncestor(Fqn oldAncestor, Fqn newAncestor) {
		if (!isChildOf(oldAncestor))
			throw new IllegalArgumentException("Old ancestor must be an ancestor of the current Fqn!");
		Fqn subFqn = this.getSubFqn(oldAncestor.size(), size());
		return Fqn.fromRelativeFqn(newAncestor, subFqn);
	}

	public static class Externalizer extends AbstractExternalizer<Fqn> {
		@Override
		public void writeObject(ObjectOutput output, Fqn fqn) throws IOException {
			output.writeInt(fqn.elements.length);
			for (Object element : fqn.elements)
				output.writeObject(element);
		}

		@Override
		public Fqn readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			int size = input.readInt();
			Object[] elements = new Object[size];
			for (int i = 0; i < size; i++)
				elements[i] = input.readObject();
			return new Fqn(elements);
		}

		@Override
		public Set<Class<? extends Fqn>> getTypeClasses() {
			return Util.<Class<? extends Fqn>> asSet(Fqn.class);
		}
	}

	public String name() {
		return ObjectUtil.toString(getLastElement()) ;
	}

	public String startWith() {
		return isRoot() ? "/*" : toString() + "/*";
	}
}