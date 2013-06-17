package net.ion.craken.node;

import java.util.Map;

import net.ion.craken.node.crud.ReadChildren;
import net.ion.framework.db.Rows;


public interface ReadNode extends NodeCommon<ReadNode> {

	
	
	<T> T toBean(Class<T> clz);

	Map<String, Object> toPropertyMap(int descendantDepth);

	public ReadChildren children();
	
	Rows toRows(String expr);

}
