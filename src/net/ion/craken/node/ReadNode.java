package net.ion.craken.node;

import java.util.Map;

import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;


public interface ReadNode extends NodeCommon<ReadNode> {

	ReadNode ref(String relName);
	// .. common 

	IteratorList<ReadNode> refs(String relName);

	<T> T toBean(Class<T> clz);

	Map<String, Object> toPropertyMap(int descendantDepth);


	
}
