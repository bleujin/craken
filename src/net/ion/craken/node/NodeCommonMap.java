package net.ion.craken.node;

import net.ion.craken.node.crud.tree.impl.PropertyValue;

public interface NodeCommonMap<T extends NodeCommonMap<T>> {

	public PropertyValue property(String key);
	public T child(String fqn);
	
	public boolean hasChild(String fqn);
	public boolean hasProperty(String pid) ;
	public T parent();
	public boolean hasRef(String refName);
	public T ref(String refName);
}
