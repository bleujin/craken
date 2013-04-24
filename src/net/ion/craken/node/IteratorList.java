package net.ion.craken.node;

import java.util.Iterator;
import java.util.List;

public interface IteratorList<T> extends Iterator<T>{
	public List<T> toList() ;
}
