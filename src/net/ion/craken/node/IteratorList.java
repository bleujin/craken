package net.ion.craken.node;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;

public abstract class IteratorList<T> implements Iterator<T>, Iterable<T>{
	
	public abstract List<T> toList() ;
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException("readonly mode") ;
	}

	public <R>  R transform(Function<Iterator<T>, R> fn) {
		return fn.apply(this) ;
	}
	
	public abstract int count() ;
}
