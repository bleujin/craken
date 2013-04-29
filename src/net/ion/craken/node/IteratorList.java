package net.ion.craken.node;

import java.util.Iterator;
import java.util.List;

public abstract class IteratorList<T> implements Iterator<T>{
	
	public abstract List<T> toList() ;
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException("readonly mode") ;
	}
}
