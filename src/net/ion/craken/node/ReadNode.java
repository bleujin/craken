package net.ion.craken.node;

import java.util.Map;

import com.google.common.base.Function;

import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.db.Rows;


public interface ReadNode extends NodeCommon<ReadNode> {

	
	
	<T> T toBean(Class<T> clz);

	Map<String, Object> toPropertyMap(int descendantDepth);

	public ReadChildren children();
	

	Rows toRows(String... cols);


}
