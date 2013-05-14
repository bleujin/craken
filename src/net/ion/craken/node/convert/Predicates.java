package net.ion.craken.node.convert;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.ComparatorUtils;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadNode;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

public class Predicates {

	// Comparison
	public final static <T extends NodeCommon> Predicate<T> hasRelation(final String refName, final Fqn target) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				return node.hasRef(refName, target);
			}
		};
	}

	public final static <T extends NodeCommon> Predicate<T> equalValue(final String propId, final Object value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null || value == null) return false ;
				return value.equals(node.property(propId).value());
			}
		};
	}

	public final static <T extends NodeCommon> Predicate<T> inValue(final String propId, final Object value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null || value == null) return false ;
				return ArrayUtil.contains(node.property(propId).asSet().toArray(), value);
			}
		};
	}

	public static <T extends NodeCommon> Predicate<T> containsValue(final String propId, final String value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null || value == null) return false ;
				for(Object eleValue : node.property(propId).asSet()){
					if (ObjectUtil.toString(eleValue).contains(value)) return true ;
				}
				return false;
			}
		};
	}
	
	
	public static <T extends NodeCommon> Predicate<T> allValue(final String propId, final Object value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null || value == null) return false ;
				
				if (value.getClass().isArray()){
					Set saved = node.property(propId).asSet();
					int size = Array.getLength(value);
					int ii = 0 ;
					for (Object object : saved) {
						if (! ObjectUtil.equals(object, Array.get(value, ii++))){
							return false ;
						}
					}
					
					return saved.size() == size ;
				} else {
					return ArrayUtil.isEquals(node.property(propId).asSet().toArray(), value) ;
				}
			}
		} ;
	}
	
	public static <T extends NodeCommon> Predicate<T> gtValue(final String propId, final Object value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null || value == null) return false ;
				return ((Comparable)node.property(propId).value()).compareTo(value) > 0  ;
			}
		} ;
	}
	
	public static <T extends NodeCommon> Predicate<T> gteValue(final String propId, final Object value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null || value == null) return false ;
				return ((Comparable)node.property(propId).value()).compareTo(value) >= 0  ;
			}
		} ;
	}
	
	public static <T extends NodeCommon> Predicate<T> ltValue(final String propId, final Object value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null || value == null) return false ;
				return ((Comparable)node.property(propId).value()).compareTo(value) < 0  ;
			}
		} ;
	}
	
	public static <T extends NodeCommon> Predicate<T> lteValue(final String propId, final Object value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null || value == null) return false ;
				return ((Comparable)node.property(propId).value()).compareTo(value) <= 0  ;
			}
		} ;
	}
	
	public static <T extends NodeCommon> Predicate<T> neValue(final String propId, final Object value) {
		return  new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null || value == null) return false ;
				return ! value.equals(node.property(propId).value());
			}
		};
	}
	
	public static <T extends NodeCommon> Predicate<T> ninValue(final String propId, final Object value) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null || value == null) return false ;
				return ! ArrayUtil.contains(node.property(propId).asSet().toArray(), value);
			}
		};
	}
	
	
	
	
	// Element
	public static <T extends NodeCommon> Predicate<T> exists(final String propId) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				return node.hasProperty(PropertyId.normal(propId)) ;
			}
		} ;
	}
	
	public static <T extends NodeCommon> Predicate<T> type(final String propId, final Class clz) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				if (node.property(propId).value() == null ) return false ;
				return clz.isInstance(node.property(propId).value()) ;
			}
		} ;
	}
	
	public static <T extends NodeCommon> Predicate<T> size(final String propId, final int size) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				return node.property(propId).asSet().size() == size ;
			}
		} ;
	}
	


	
	
	
	
	// Logical
	
	public final static <T extends NodeCommon> Predicate<T> and(Predicate<T>... components) {
		return new AndPredicate(ListUtil.toList(components)) ;
	}

	public final static <T extends NodeCommon> Predicate<T> or(Predicate<T>... components) {
		return new OrPredicate(ListUtil.toList(components)) ;
	}

	public final static <T extends NodeCommon> Predicate<T> nor(final Predicate<T> left, final Predicate<T> right) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				return left.apply(node) ^ right.apply(node) ;
			}
		} ;
	}
	
	public final static <T extends NodeCommon> Predicate<T> not(final Predicate<T> component) {
		return new Predicate<T>() {
			@Override
			public boolean apply(T node) {
				return ! component.apply(node) ;
			}
		} ;
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
