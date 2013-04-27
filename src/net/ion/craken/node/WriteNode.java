package net.ion.craken.node;

import java.util.Map;

import net.ion.craken.tree.PropertyValue;

public interface WriteNode extends NodeCommon<WriteNode> {

	public WriteNode property(String key, Object value) ;
	
	public PropertyValue propertyIfAbsent(String key, Object value) ;
	
	public PropertyValue replace(String key, Object value) ;
	
	public boolean replace(String key, Object oldValue, Object newValue) ;
	
	public WriteNode propertyAll(Map<String, ? extends Object> map) ;
	
	public WriteNode replaceAll(Map<String, ? extends Object> newMap) ;
	
	
	public WriteNode unset(String key) ;
	
	public WriteNode clear() ;
	
	public WriteNode addChild(String relativeFqn) ;
	
	public boolean removeChild(String fqn) ;
	
	public void removeChildren() ;

	public WriteNode refToFirst(String refName, String fqn);

	public WriteNode refToLast(String refName, String fqn);


}
