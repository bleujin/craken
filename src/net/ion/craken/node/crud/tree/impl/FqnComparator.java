package net.ion.craken.node.crud.tree.impl;

import java.io.Serializable;
import java.util.Comparator;

import net.ion.craken.node.crud.tree.Fqn;

// @Immutable
public class FqnComparator implements Comparator<Fqn>, Serializable {
	public static final FqnComparator INSTANCE = new FqnComparator();
	private static final long serialVersionUID = -1357631755443829281L;

	/**
	 * Returns -1 if the first comes before; 0 if they are the same; 1 if the second Fqn comes before. <code>null</code> always comes first.
	 */
	@Override
	public int compare(Fqn fqn1, Fqn fqn2) {
		int s1 = fqn1.size();
		int s2 = fqn2.size();

		if (s1 == 0) {
			return (s2 == 0) ? 0 : -1;
		}

		if (s2 == 0) {
			return 1;
		}

		int size = Math.min(s1, s2);

		for (int i = 0; i < size; i++) {
			Object e1 = fqn1.get(i);
			Object e2 = fqn2.get(i);
			if (e1 == e2) {
				continue;
			}
			if (e1 == null) {
				return 0;
			}
			if (e2 == null) {
				return 1;
			}
			if (!e1.equals(e2)) {
				int c = compareElements(e1, e2);
				if (c != 0) {
					return c;
				}
			}
		}

		return s1 - s2;
	}

	/**
	 * Compares two Fqn elements. If e1 and e2 are the same class and e1 implements Comparable, returns e1.compareTo(e2). 
	 * Otherwise, returns e1.toString().compareTo(e2.toString()).
	 */
	@SuppressWarnings("unchecked")
	private int compareElements(Object e1, Object e2) {
		if (e1.getClass() == e2.getClass() && e1 instanceof Comparable) {
			return ((Comparable<Object>) e1).compareTo(e2);
		} else {
			return e1.toString().compareTo(e2.toString());
		}
	}

}