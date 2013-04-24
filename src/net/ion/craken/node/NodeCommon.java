package net.ion.craken.node;

import java.util.Set;

import com.google.common.base.Optional;

import net.ion.craken.tree.Fqn;

public interface NodeCommon<T extends NodeCommon> {
	
	public final static String IDProp = "__id" ;
	public final static String NameProp = "__name";
	
	public Fqn fqn();

	public int dataSize();

	public T parent();

	public boolean hasChild(String fqn);

	public T child(String fqn);

	public Set<String> childrenNames();

	public IteratorList<T> children();

	public Set<String> keys();

	public Object property(String key);
	
	public Optional optional(String key) ;
	
	public Object id() ;

}
