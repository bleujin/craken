package net.ion.craken.node.crud.tree.impl;

import net.ion.craken.node.crud.tree.Fqn;

import org.infinispan.atomic.AtomicMap;

public interface ProxyHandler {

	public final static ProxyHandler BLANK = new ProxyHandler() {
		@Override
		public AtomicMap<Object, Fqn> handleStructure(TreeNodeKey struKey, AtomicMap<Object, Fqn> created) {
			return created;
		}
		
		@Override
		public AtomicMap<PropertyId, PropertyValue> handleData(TreeNodeKey dataKey, AtomicMap<PropertyId, PropertyValue> created) {
			return created;
		}
	};
	
	
	AtomicMap<PropertyId, PropertyValue> handleData(TreeNodeKey dataKey, AtomicMap<PropertyId, PropertyValue> created) ;
	AtomicMap<Object, Fqn> handleStructure(TreeNodeKey struKey, AtomicMap<Object, Fqn> created) ;
	
}
