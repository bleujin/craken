package net.ion.craken.node.crud.util;

import java.util.Iterator;

public class PairIterator<K, T> implements Iterator<T> {

	private K key ;
	private Iterator<T> inner ;
	public PairIterator(K key, Iterator<T> inner){
		this.key = key ;
		this.inner = inner ;
	}
	
	public K key(){
		return key ;
	}
	
	public Iterator<T> value(){
		return inner ;
	}

	@Override
	public boolean hasNext() {
		return inner.hasNext();
	}

	@Override
	public T next() {
		return inner.next();
	}

	@Override
	public void remove() {
		inner.remove(); 
	}

}
