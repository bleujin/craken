package net.ion.craken.node;

import java.util.Map;

import com.google.common.base.Function;

import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;


public interface ReadNode extends NodeCommon<ReadNode> {

	<T> T transformer(Function<ReadNode, T> transformer) ;
	
	<T> T toBean(Class<T> clz);

	Map<String, Object> toPropertyMap(int descendantDepth);


	
}
