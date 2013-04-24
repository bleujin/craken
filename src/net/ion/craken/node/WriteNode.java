package net.ion.craken.node;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

public interface WriteNode extends NodeCommon<WriteNode> {

	public WriteNode property(String key, Object value) ;
	
	public Object propertyIfAbsent(String key, Object value) ;
	
	public Object replace(String key, Object value) ;
	
	public boolean replace(String key, Object oldValue, Object newValue) ;
	
	public WriteNode propertyAll(Map<String, ? extends Object> map) ;
	
	public WriteNode replaceAll(Map<String, ? extends Object> newMap) ;
	
	
	public WriteNode unset(String key) ;
	
	public WriteNode clear() ;
	
	public WriteNode addChild(String relativeFqn) ;
	
	public boolean removeChild(String fqn) ;
	
	public void removeChildren() ;
	
	

}
