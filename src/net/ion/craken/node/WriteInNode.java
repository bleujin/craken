package net.ion.craken.node;

import java.io.Serializable;
import java.util.Map;

import net.ion.framework.util.MapUtil;

public class WriteInNode implements Serializable{

	private static final long serialVersionUID = 1704957555176607028L;
	
	private Map<String, Object> map = MapUtil.newMap() ;
	private WriteNode parent ;
	public WriteInNode property(String key, Object value){
		map.put(key, value) ;
		return this ;
	}
	
	
	public Object property(String key){
		return map.get(key) ;
	}


	public WriteInNode by(WriteNode wnode) {
		this.parent = wnode ;
		return this;
	}

	
}
