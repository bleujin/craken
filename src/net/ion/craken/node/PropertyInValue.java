package net.ion.craken.node;

import java.io.Serializable;
import java.util.Map;

import net.ion.craken.tree.PropertyValue;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.MapUtil;

public interface PropertyInValue extends Serializable{

	
	public PropertyInValue property(String key, Object value) ;
	
	
	public PropertyValue property(String key) ;


}

class PropertyWriteInValue implements PropertyInValue{

	private static final long serialVersionUID = 1704957555176607028L;
	
	private JsonObject values ;
	
	public PropertyWriteInValue(PropertyValue pvalue) {
		this.values = pvalue.jsonElement().getAsJsonObject() ;
	}


	public PropertyInValue property(String key, Object value){
		values.put(key, value) ;
		return this ;
	}
	
	
	public PropertyValue property(String key){
		return PropertyValue.createPrimitive(values.get(key).getAsJsonPrimitive().getValue()) ;
	}

	
}