package net.ion.craken.node.crud;

public interface WriteChildrenEach<T> {
	
	public T handle(WriteChildrenIterator citer) ;
}
