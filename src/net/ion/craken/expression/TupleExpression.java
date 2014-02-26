package net.ion.craken.expression;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.SetUtil;

public class TupleExpression extends ValueObject implements Expression {
	public final List<Expression> expressions;

	public TupleExpression(List<Expression> expressions) {
		this.expressions = Collections.unmodifiableList(expressions);
	}

	public static TupleExpression of(Expression... expressions) {
		return new TupleExpression(Arrays.asList(expressions));
	}

	@Override
	public Comparable value(NodeCommon node) {
		ComparableSet result = new ComparableSet();
		for (Expression exp : expressions) {
			result.add(exp.value(node)) ;
		}

		return result;
	}
	
	public List<Expression> expressions(){
		return expressions ;
	}


}

class ComparableSet implements Set<Comparable>, Comparable<ComparableSet>{

	private Set<Comparable> inner = SetUtil.newOrdereddSet() ;
	@Override
	public boolean add(Comparable e) {
		return inner.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends Comparable> c) {
		return inner.addAll(c);
	}

	@Override
	public void clear() {
		inner.clear() ;
	}

	@Override
	public boolean contains(Object o) {
		return inner.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return inner.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return inner.isEmpty();
	}

	@Override
	public Iterator<Comparable> iterator() {
		return inner.iterator() ;
	}

	@Override
	public boolean remove(Object o) {
		return inner.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return inner.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return inner.retainAll(c);
	}

	@Override
	public int size() {
		return inner.size();
	}

	@Override
	public Object[] toArray() {
		return inner.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return inner.toArray(a);
	}

	public boolean equals(Object o){
		return inner.equals(o) ;
	}
	
	public String toString(){
		return inner.toString();
	}
	
	@Override
	public int compareTo(ComparableSet o) {
		Comparable[] thisArray = toArray(new Comparable[0]);
		Comparable[] thatArray = o.toArray(new Comparable[0]);
		for (int i=0 ; i < thisArray.length ; i++) {
			if (thatArray.length <= i) return 1 ;
			if (thisArray[i].compareTo(thatArray[i]) != 0) return thisArray[i].compareTo(thatArray[i]) ;  
		}
		if (thatArray.length > thisArray.length) return -1 ;
		return 0 ;
	}

}