package net.ion.craken.node.convert.rows;

import java.util.Map;

import net.ion.framework.util.MapUtil;

public class FieldContext {

	private Map<String, Object> attrs;
	public FieldContext(){
		this(MapUtil.EMPTY) ;
	}

	public FieldContext(Map<String, Object> attrs){
		this.attrs = attrs ;
	}
	
	public Object getAttribute(String name){
		return attrs.get(name) ;
	}

	public <T> T getAttribute(Class<T> clz){
		return (T) attrs.get(clz.getCanonicalName()) ;
	}

}
