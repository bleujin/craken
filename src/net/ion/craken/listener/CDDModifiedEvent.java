package net.ion.craken.listener;

import java.util.Map;

import net.ion.craken.node.TouchedRow;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.ObjectUtil;

public class CDDModifiedEvent {

	private TreeNodeKey key;
	private Map<PropertyId, PropertyValue> value;
	
	public CDDModifiedEvent(TreeNodeKey dataKey, Map<PropertyId, PropertyValue> map) {
		this.key = dataKey ;
		this.value = map ;
	}

	public final static CDDModifiedEvent create(TouchedRow trow){
		return new CDDModifiedEvent(trow.target().dataKey(), trow.source().toMap()) ;
	}
	
	public TreeNodeKey getKey(){
		return key ;
	}
	
	public Map<PropertyId, PropertyValue> getValue(){
		return value ;
	}
	
	public PropertyValue property(String propId){
		PropertyValue result = value.get(PropertyId.normal(propId)) ;
		return ObjectUtil.coalesce(result, PropertyValue.NotFound) ;
	}
}
