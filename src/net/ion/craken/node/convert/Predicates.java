package net.ion.craken.node.convert;

import java.io.Serializable;
import java.util.List;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadNode;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.ListUtil;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

public class Predicates {

	public final static <T extends NodeCommon> Predicate<T> hasRelation(final String refName, final Fqn target) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				return node.hasRef(refName, target);
			}
		};
	}

	public final static <T extends NodeCommon> Predicate<T> propertyEqual(final String propId, final Object value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				return value.equals(node.property(propId).value());
			}
		};
	}

	public final static <T extends NodeCommon> Predicate<T> propertyHasValue(final String propId, final Object value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				return ArrayUtil.contains(node.property(propId).asSet().toArray(), value);
			}
		};
	}

	public static <T extends NodeCommon> Predicate<T> propertyContains(final String propId, final String value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				return node.property(propId).stringValue().contains(value) ;
			}
		};
	}

	
	
	public final static <T extends NodeCommon> Predicate<T> and(Predicate<T>... component) {
		return new AndPredicate(ListUtil.toList(component)) ;
	}

	public final static <T extends NodeCommon> Predicate<T> or(Predicate<T>... component) {
		return new OrPredicate(ListUtil.toList(component)) ;
	}
	

	
	private static class OrPredicate<T> implements Predicate<T>, Serializable {

		private final List<Predicate> components;
		private static final long serialVersionUID = 0L;

		private OrPredicate(List<Predicate> components) {
			this.components = components;
		}

		public boolean apply(Object t) {
			for (int i = 0; i < components.size(); i++)
				if (((Predicate) components.get(i)).apply(t))
					return true;

			return false;
		}

		public int hashCode() {
			return components.hashCode() + 87855567;
		}

		public boolean equals(Object obj) {
			if (obj instanceof OrPredicate) {
				OrPredicate that = (OrPredicate) obj;
				return components.equals(that.components);
			} else {
				return false;
			}
		}

	}

	private static class AndPredicate<T> implements Predicate<T>, Serializable {

		private final List<Predicate> components;
		private static final long serialVersionUID = 0L;

		private AndPredicate(List<Predicate> components) {
			this.components = components;
		}

		public boolean apply(Object t) {
			for (int i = 0; i < components.size(); i++)
				if (!((Predicate) components.get(i)).apply(t))
					return false;

			return true;
		}

		public int hashCode() {
			return components.hashCode() + 306654252;
		}

		public boolean equals(Object obj) {
			if (obj instanceof AndPredicate) {
				AndPredicate that = (AndPredicate) obj;
				return components.equals(that.components);
			} else {
				return false;
			}
		}

	}

	private static class NotPredicate<T> implements Predicate<T>, Serializable {

		final Predicate predicate;
		private static final long serialVersionUID = 0L;

		NotPredicate(Predicate predicate) {
			this.predicate = (Predicate) Preconditions.checkNotNull(predicate);
		}

		public boolean apply(Object t) {
			return !predicate.apply(t);
		}

		public int hashCode() {
			return ~predicate.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj instanceof NotPredicate) {
				NotPredicate that = (NotPredicate) obj;
				return predicate.equals(that.predicate);
			} else {
				return false;
			}
		}

		public String toString() {
			return (new StringBuilder()).append("Not(").append(predicate.toString()).append(")").toString();
		}

	}


}
